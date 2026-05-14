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

    private UUID grainId; // grão que está carregando nesta viagem

    public Truck() {}

    public Truck(String plate, Double tareWeight, UUID grainId) {
        this.plate = plate;
        this.tareWeight = tareWeight;
        this.grainId = grainId;
    }

    public UUID getId() { return id; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public Double getTareWeight() { return tareWeight; }
    public void setTareWeight(Double tareWeight) { this.tareWeight = tareWeight; }
    public UUID getGrainId() { return grainId; }
    public void setGrainId(UUID grainId) { this.grainId = grainId; }
}
