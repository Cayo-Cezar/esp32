package com.grainscale.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID truckId;

    @Column(nullable = false)
    private UUID grainId;

    @Column(nullable = false)
    private UUID scaleId;

    @Column(nullable = false)
    private UUID branchId;

    private Double grossWeight;
    private Double netWeight;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalPurchasePrice;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalSalePrice;

    private Double profitMargin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    public Transaction() {}

    // Getters e Setters
    public UUID getId() { return id; }
    public UUID getTruckId() { return truckId; }
    public void setTruckId(UUID truckId) { this.truckId = truckId; }
    public UUID getGrainId() { return grainId; }
    public void setGrainId(UUID grainId) { this.grainId = grainId; }
    public UUID getScaleId() { return scaleId; }
    public void setScaleId(UUID scaleId) { this.scaleId = scaleId; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
    public Double getGrossWeight() { return grossWeight; }
    public void setGrossWeight(Double grossWeight) { this.grossWeight = grossWeight; }
    public Double getNetWeight() { return netWeight; }
    public void setNetWeight(Double netWeight) { this.netWeight = netWeight; }
    public BigDecimal getTotalPurchasePrice() { return totalPurchasePrice; }
    public void setTotalPurchasePrice(BigDecimal totalPurchasePrice) { this.totalPurchasePrice = totalPurchasePrice; }
    public BigDecimal getTotalSalePrice() { return totalSalePrice; }
    public void setTotalSalePrice(BigDecimal totalSalePrice) { this.totalSalePrice = totalSalePrice; }
    public Double getProfitMargin() { return profitMargin; }
    public void setProfitMargin(Double profitMargin) { this.profitMargin = profitMargin; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
