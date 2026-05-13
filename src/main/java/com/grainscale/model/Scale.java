package com.grainscale.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "scales")
public class Scale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String externalId; // ID do ESP32

    @Column(nullable = false)
    private UUID branchId;

    public Scale() {}

    public Scale(String externalId, UUID branchId) {
        this.externalId = externalId;
        this.branchId = branchId;
    }

    public UUID getId() { return id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
}
