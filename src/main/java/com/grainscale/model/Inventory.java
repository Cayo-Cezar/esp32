package com.grainscale.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "inventories")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID branchId;

    @Column(nullable = false)
    private UUID grainId;

    @Column(nullable = false)
    private Double weightKg;

    public Inventory() {}

    public Inventory(UUID branchId, UUID grainId, Double weightKg) {
        this.branchId = branchId;
        this.grainId = grainId;
        this.weightKg = weightKg;
    }

    public UUID getId() { return id; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
    public UUID getGrainId() { return grainId; }
    public void setGrainId(UUID grainId) { this.grainId = grainId; }
    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
}
