package com.grainscale.service;

import com.grainscale.config.RedisStreamConfig;
import com.grainscale.exception.ResourceNotFoundException;
import com.grainscale.model.Grain;
import com.grainscale.model.Transaction;
import com.grainscale.model.TransactionStatus;
import com.grainscale.model.Truck;
import com.grainscale.model.Scale;
import com.grainscale.model.Inventory;
import com.grainscale.repository.GrainRepository;
import com.grainscale.repository.ScaleRepository;
import com.grainscale.repository.TransactionRepository;
import com.grainscale.repository.TruckRepository;
import com.grainscale.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;

/**
 * Consumer do Redis Stream + Regras de Negócio.
 *
 * Lê os eventos de peso estabilizado, calcula peso líquido,
 * aplica margem de lucro dinâmica e salva a transação no banco.
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private static final double MARGIN_MIN = 0.05;  // 5%
    private static final double MARGIN_MAX = 0.20;  // 20%

    private final StringRedisTemplate redisTemplate;
    private final TruckRepository truckRepository;
    private final GrainRepository grainRepository;
    private final ScaleRepository scaleRepository;
    private final TransactionRepository transactionRepository;
    private final InventoryRepository inventoryRepository;

    public TransactionService(StringRedisTemplate redisTemplate,
                              TruckRepository truckRepository,
                              GrainRepository grainRepository,
                              ScaleRepository scaleRepository,
                              TransactionRepository transactionRepository,
                              InventoryRepository inventoryRepository) {
        this.redisTemplate = redisTemplate;
        this.truckRepository = truckRepository;
        this.grainRepository = grainRepository;
        this.scaleRepository = scaleRepository;
        this.transactionRepository = transactionRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Consome eventos do Redis Stream a cada 1 segundo.
     * Lê até 10 eventos por vez e processa cada um.
     */
    @Scheduled(fixedDelay = 1000)
    public void consumeStabilizedWeights() {
        try {
            var records = redisTemplate.opsForStream().read(
                    Consumer.from(RedisStreamConfig.GROUP_NAME, "worker-1"),
                    org.springframework.data.redis.connection.stream.StreamReadOptions.empty().count(10),
                    StreamOffset.create(RedisStreamConfig.STREAM_KEY, ReadOffset.lastConsumed())
            );

            if (records == null || records.isEmpty()) return;

            for (var record : records) {
                try {
                    processRecord(record);
                    // Confirma que processou com sucesso (acknowledge)
                    redisTemplate.opsForStream().acknowledge(
                            RedisStreamConfig.STREAM_KEY, RedisStreamConfig.GROUP_NAME, record.getId());
                } catch (Exception e) {
                    log.error("Erro ao processar registro {}: {}", record.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            // Stream ou grupo não existe ainda — normal se nenhum peso foi publicado
        }
    }

    /**
     * Processa um registro do Redis Stream e cria a transação.
     */
    private void processRecord(MapRecord<String, Object, Object> record) {
        Map<Object, Object> data = record.getValue();

        String plate = (String) data.get("plate");
        String scaleId = (String) data.get("scaleId");
        double grossWeight = Double.parseDouble((String) data.get("stabilizedWeight"));

        // 1. Busca o caminhão pela placa
        Truck truck = truckRepository.findByPlate(plate)
                .orElseThrow(() -> new ResourceNotFoundException("Caminhão não encontrado: " + plate));

        // 2. Busca a balança pelo ID externo
        Scale scale = scaleRepository.findByExternalId(scaleId)
                .orElseThrow(() -> new ResourceNotFoundException("Balança não encontrada: " + scaleId));

        // 3. Busca o grão vinculado ao caminhão
        if (truck.getGrainId() == null) {
            throw new IllegalStateException("Caminhão " + plate + " não tem grão atribuído");
        }
        Grain grain = grainRepository.findById(truck.getGrainId())
                .orElseThrow(() -> new ResourceNotFoundException("Grão não encontrado: " + truck.getGrainId()));

        // 4. Calcula peso líquido (bruto - tara)
        double netWeight = grossWeight - truck.getTareWeight();
        if (netWeight <= 0) {
            log.warn("Peso líquido negativo para placa {}: bruto={} - tara={}", plate, grossWeight, truck.getTareWeight());
            saveErrorTransaction(truck, grain, scale, grossWeight);
            return;
        }

        // Recupera ou cria o inventário para a filial e o grão
        Inventory inventory = inventoryRepository.findByBranchIdAndGrainId(scale.getBranchId(), grain.getId())
                .orElse(new Inventory(scale.getBranchId(), grain.getId(), 0.0));

        double availableWeightKg = inventory.getWeightKg();

        // 5. Calcula margem dinâmica baseada na escassez do grão (disponibilidade na doca)
        double profitMargin = calculateDynamicMargin(availableWeightKg);

        // 6. Calcula preços
        double netWeightInTons = netWeight / 1000.0;
        BigDecimal purchasePrice = grain.getPurchasePricePerTon()
                .multiply(BigDecimal.valueOf(netWeightInTons))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal salePrice = purchasePrice
                .multiply(BigDecimal.valueOf(1 + profitMargin))
                .setScale(2, RoundingMode.HALF_UP);

        // 7. Salva a transação
        Transaction transaction = new Transaction();
        transaction.setTruckId(truck.getId());
        transaction.setGrainId(grain.getId());
        transaction.setScaleId(scale.getId());
        transaction.setBranchId(scale.getBranchId());
        transaction.setPlate(plate);
        transaction.setGrossWeight(grossWeight);
        transaction.setTareWeight(truck.getTareWeight());
        transaction.setNetWeight(netWeight);
        transaction.setTotalPurchasePrice(purchasePrice);
        transaction.setTotalSalePrice(salePrice);
        transaction.setProfitMargin(profitMargin * 100); // armazena em %
        transaction.setStatus(TransactionStatus.PROCESSED);
        transaction.setCreatedAt(Instant.now());

        transactionRepository.save(transaction);

        // 8. Atualiza o estoque
        inventory.setWeightKg(inventory.getWeightKg() + netWeight);
        inventoryRepository.save(inventory);

        log.info("✅ Transação processada: placa={}, grão={}, pesoLíquido={}kg, compra=R${}, venda=R${}, margem={}%",
                plate, grain.getName(), String.format("%.2f", netWeight),
                purchasePrice, salePrice, String.format("%.1f", profitMargin * 100));
    }

    /**
     * Calcula a margem de lucro dinâmica baseada na escassez do grão na doca.
     *
     * Quanto menos grão disponível, maior a margem.
     * - 0 kg → 20% (máxima)
     * - 100.000+ kg → 5% (mínima)
     */
    private double calculateDynamicMargin(double availableWeightKg) {
        double SCARCITY_THRESHOLD_KG = 100000.0; // 100 toneladas

        // Interpolação linear: margem diminui conforme mais estoque existe
        double ratio = Math.min(availableWeightKg / SCARCITY_THRESHOLD_KG, 1.0);
        double margin = MARGIN_MAX - (ratio * (MARGIN_MAX - MARGIN_MIN));

        log.info("Margem calculada: {} kg disponíveis deste grão na doca → margem={}%",
                availableWeightKg, String.format("%.1f", margin * 100));

        return margin;
    }

    /**
     * Salva uma transação com status ERROR quando o peso líquido é inválido.
     */
    private void saveErrorTransaction(Truck truck, Grain grain, Scale scale, double grossWeight) {
        Transaction transaction = new Transaction();
        transaction.setTruckId(truck.getId());
        transaction.setGrainId(grain.getId());
        transaction.setScaleId(scale.getId());
        transaction.setBranchId(scale.getBranchId());
        transaction.setPlate(truck.getPlate());
        transaction.setGrossWeight(grossWeight);
        transaction.setTareWeight(truck.getTareWeight());
        transaction.setStatus(TransactionStatus.ERROR);
        transaction.setCreatedAt(Instant.now());
        transactionRepository.save(transaction);
    }
}
