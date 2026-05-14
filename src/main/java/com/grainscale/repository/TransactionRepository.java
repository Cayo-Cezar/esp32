package com.grainscale.repository;

import com.grainscale.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByGrainId(UUID grainId);
    List<Transaction> findByTruckId(UUID truckId);
    List<Transaction> findByBranchId(UUID branchId);
    List<Transaction> findByPlate(String plate);

    // Contadores para relatórios
    long countByGrainId(UUID grainId);
    long countByBranchId(UUID branchId);

    @Query("SELECT SUM(t.netWeight) FROM Transaction t WHERE t.grainId = :grainId AND t.status = 'PROCESSED'")
    Double sumNetWeightByGrainId(UUID grainId);

    @Query("SELECT SUM(t.netWeight) FROM Transaction t WHERE t.branchId = :branchId AND t.status = 'PROCESSED'")
    Double sumNetWeightByBranchId(UUID branchId);

    @Query("SELECT SUM(t.totalPurchasePrice) FROM Transaction t WHERE t.status = 'PROCESSED'")
    java.math.BigDecimal sumTotalPurchasePrice();

    @Query("SELECT SUM(t.totalSalePrice) FROM Transaction t WHERE t.status = 'PROCESSED'")
    java.math.BigDecimal sumTotalSalePrice();

    @Query("SELECT AVG(t.profitMargin) FROM Transaction t WHERE t.status = 'PROCESSED'")
    Double avgProfitMargin();
}
