package com.grainscale.service;

import com.grainscale.dto.StabilizedWeightEvent;
import com.grainscale.dto.WeighingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia as Sliding Windows de pesagem.
 *
 * Para cada caminhão na balança, mantém uma janela com as leituras de peso.
 * Quando o peso estabiliza (variância baixa por >= 3 segundos), publica no
 * Redis.
 * Se o caminhão fugir antes dos 3 segundos, descarta tudo.
 */
@Service
public class WeighingService {

    private static final Logger log = LoggerFactory.getLogger(WeighingService.class);

    // Mapa em memória: chave "scaleId:plate" → janela de leituras
    private final Map<String, WeighingWindow> windows = new ConcurrentHashMap<>();

    // Chaves que acabaram de estabilizar — pra diferenciar "sobras" de "fuga real"
    private final Set<String> recentlyStabilized = ConcurrentHashMap.newKeySet();

    private final WeighingPublisher publisher;

    @Value("${weighing.window-duration-ms:3000}")
    private long windowDurationMs;

    @Value("${weighing.variance-threshold:50.0}")
    private double varianceThreshold;

    @Value("${weighing.idle-timeout-ms:500}")
    private long idleTimeoutMs;

    public WeighingService(WeighingPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * Processa uma leitura recebida do ESP32.
     * Chamado pelo Controller a cada POST (~100ms).
     */
    public void processReading(WeighingRequest request) {
        String key = request.id() + ":" + request.plate();

        // Pega ou cria a janela para esse caminhão nessa balança
        WeighingWindow window = windows.computeIfAbsent(key,
                k -> new WeighingWindow(request.id(), request.plate()));

        // Adiciona a leitura
        window.addReading(request.weight());

        // Verifica se já passou o tempo mínimo da janela (3 segundos)
        if (window.getDurationMs() >= windowDurationMs) {
            double variance = window.getVariance();

            if (variance <= varianceThreshold) {
                // Peso estabilizou! Publica no Redis Stream
                var event = new StabilizedWeightEvent(
                        window.getScaleId(),
                        window.getPlate(),
                        window.getMeanWeight(),
                        window.getReadingCount(),
                        Instant.now());

                publisher.publish(event);

                // Remove a janela (pesagem concluída) e marca como recém-estabilizada
                windows.remove(key);
                recentlyStabilized.add(key);

                log.info("Janela removida [{}]: peso estabilizado em {}kg", key, event.stabilizedWeight());
            }
        }
    }

    /**
     * Verifica periodicamente se algum caminhão "fugiu" da balança.
     *
     * Se uma janela não recebe leituras há mais de 500ms, significa que
     * o caminhão saiu. Diferencia entre:
     * - Registros residuais (sobras após estabilização já concluída)
     * - Fuga prematura real (caminhão saiu antes de 3 segundos)
     * - Peso instável (ficou tempo suficiente mas oscilou demais)
     */
    @Scheduled(fixedDelayString = "${weighing.escape-check-interval-ms:500}")
    public void checkForEscapes() {
        Iterator<Map.Entry<String, WeighingWindow>> it = windows.entrySet().iterator();

        while (it.hasNext()) {
            var entry = it.next();
            WeighingWindow window = entry.getValue();

            if (window.getIdleMs() > idleTimeoutMs) {
                if (recentlyStabilized.remove(entry.getKey())) {
                    // Sobras de uma pesagem que já foi concluída com sucesso
                    log.info("Descartando {} registros residuais [{}] — pesagem já concluída",
                            window.getReadingCount(), entry.getKey());

                } else if (window.getDurationMs() < windowDurationMs) {
                    // Fuga prematura real — caminhão saiu antes de 3 segundos
                    log.warn("⚠️ Fuga prematura detectada [{}]: {}ms de dados ({} leituras) — descartando",
                            entry.getKey(), window.getDurationMs(), window.getReadingCount());

                } else {
                    // Caminhão ficou tempo suficiente mas peso não estabilizou
                    log.warn("⚠️ Peso não estabilizou [{}]: variância={} (limite={}) — descartando",
                            entry.getKey(), window.getVariance(), varianceThreshold);
                }

                it.remove();
            }
        }
    }

    /** Retorna quantas janelas estão ativas (para monitoramento) */
    public int getActiveWindowCount() {
        return windows.size();
    }
}
