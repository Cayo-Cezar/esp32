package com.grainscale.controller;

import com.grainscale.model.Grain;
import com.grainscale.model.Branch;
import com.grainscale.repository.BranchRepository;
import com.grainscale.repository.GrainRepository;
import com.grainscale.repository.TransactionRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Relatórios e estatísticas administrativas.
 *
 * Fornece dados consolidados para análise de operações:
 * totais por grão, por filial, margens e resumo geral.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final TransactionRepository transactionRepository;
    private final GrainRepository grainRepository;
    private final BranchRepository branchRepository;

    public ReportController(TransactionRepository transactionRepository,
                            GrainRepository grainRepository,
                            BranchRepository branchRepository) {
        this.transactionRepository = transactionRepository;
        this.grainRepository = grainRepository;
        this.branchRepository = branchRepository;
    }

    /**
     * Resumo geral: totais de compra, venda, lucro e margem média.
     * GET /api/reports/summary
     */
    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        long totalTransactions = transactionRepository.count();
        BigDecimal totalPurchase = transactionRepository.sumTotalPurchasePrice();
        BigDecimal totalSale = transactionRepository.sumTotalSalePrice();
        Double avgMargin = transactionRepository.avgProfitMargin();

        summary.put("totalTransactions", totalTransactions);
        summary.put("totalPurchasePrice", totalPurchase != null ? totalPurchase : BigDecimal.ZERO);
        summary.put("totalSalePrice", totalSale != null ? totalSale : BigDecimal.ZERO);
        summary.put("totalProfit", totalSale != null && totalPurchase != null
                ? totalSale.subtract(totalPurchase) : BigDecimal.ZERO);
        summary.put("avgProfitMargin", avgMargin != null ? String.format("%.1f%%", avgMargin) : "0%");

        return summary;
    }

    /**
     * Relatório por tipo de grão: quantidade, peso total e valores.
     * GET /api/reports/by-grain
     */
    @GetMapping("/by-grain")
    public List<Map<String, Object>> getByGrain() {
        List<Map<String, Object>> report = new ArrayList<>();

        for (Grain grain : grainRepository.findAll()) {
            var transactions = transactionRepository.findByGrainId(grain.getId());
            if (transactions.isEmpty()) continue;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("grainName", grain.getName());
            entry.put("purchasePricePerTon", grain.getPurchasePricePerTon());
            entry.put("totalTransactions", transactions.size());

            Double totalWeight = transactionRepository.sumNetWeightByGrainId(grain.getId());
            entry.put("totalNetWeightKg", totalWeight != null ? totalWeight : 0);

            BigDecimal totalPurchase = BigDecimal.ZERO;
            BigDecimal totalSale = BigDecimal.ZERO;
            for (var t : transactions) {
                if (t.getTotalPurchasePrice() != null) totalPurchase = totalPurchase.add(t.getTotalPurchasePrice());
                if (t.getTotalSalePrice() != null) totalSale = totalSale.add(t.getTotalSalePrice());
            }

            entry.put("totalPurchasePrice", totalPurchase);
            entry.put("totalSalePrice", totalSale);
            entry.put("totalProfit", totalSale.subtract(totalPurchase));

            report.add(entry);
        }

        return report;
    }

    /**
     * Relatório por filial: quantidade de transações e peso processado.
     * GET /api/reports/by-branch
     */
    @GetMapping("/by-branch")
    public List<Map<String, Object>> getByBranch() {
        List<Map<String, Object>> report = new ArrayList<>();

        for (Branch branch : branchRepository.findAll()) {
            var transactions = transactionRepository.findByBranchId(branch.getId());
            if (transactions.isEmpty()) continue;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("branchName", branch.getName());
            entry.put("location", branch.getLocation());
            entry.put("totalTransactions", transactions.size());

            Double totalWeight = transactionRepository.sumNetWeightByBranchId(branch.getId());
            entry.put("totalNetWeightKg", totalWeight != null ? totalWeight : 0);

            BigDecimal totalPurchase = BigDecimal.ZERO;
            BigDecimal totalSale = BigDecimal.ZERO;
            for (var t : transactions) {
                if (t.getTotalPurchasePrice() != null) totalPurchase = totalPurchase.add(t.getTotalPurchasePrice());
                if (t.getTotalSalePrice() != null) totalSale = totalSale.add(t.getTotalSalePrice());
            }

            entry.put("totalPurchasePrice", totalPurchase);
            entry.put("totalSalePrice", totalSale);
            entry.put("totalProfit", totalSale.subtract(totalPurchase));

            report.add(entry);
        }

        return report;
    }

    /**
     * Relatório por caminhão (placa): transações realizadas.
     * GET /api/reports/by-truck/{plate}
     */
    @GetMapping("/by-truck/{plate}")
    public Map<String, Object> getByTruck(@PathVariable String plate) {
        var transactions = transactionRepository.findByPlate(plate.toUpperCase());

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("plate", plate.toUpperCase());
        report.put("totalTrips", transactions.size());

        BigDecimal totalValue = BigDecimal.ZERO;
        double totalWeight = 0;
        for (var t : transactions) {
            if (t.getTotalPurchasePrice() != null) totalValue = totalValue.add(t.getTotalPurchasePrice());
            if (t.getNetWeight() != null) totalWeight += t.getNetWeight();
        }

        report.put("totalNetWeightKg", totalWeight);
        report.put("totalCargoValue", totalValue);

        return report;
    }
}
