package com.grainscale.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "grains")
public class Grain {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePricePerTon;

    public Grain() {}

    public Grain(String name, BigDecimal purchasePricePerTon) {
        this.name = name;
        this.purchasePricePerTon = purchasePricePerTon;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPurchasePricePerTon() { return purchasePricePerTon; }
    public void setPurchasePricePerTon(BigDecimal purchasePricePerTon) { this.purchasePricePerTon = purchasePricePerTon; }
}
