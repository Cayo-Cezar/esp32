package com.grainscale.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "trucks")
public class Truck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String plate;

    @Column(nullable = false)
    private Double tareWeight;

    public Truck() {}

    public Truck(String plate, Double tareWeight) {
        this.plate = plate;
        this.tareWeight = tareWeight;
    }

    public UUID getId() { return id; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public Double getTareWeight() { return tareWeight; }
    public void setTareWeight(Double tareWeight) { this.tareWeight = tareWeight; }
}
