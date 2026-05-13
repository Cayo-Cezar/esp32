package com.grainscale.repository;

import com.grainscale.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByGrainId(UUID grainId);
    List<Transaction> findByTruckId(UUID truckId);
}
