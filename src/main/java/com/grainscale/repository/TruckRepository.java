package com.grainscale.repository;

import com.grainscale.model.Truck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TruckRepository extends JpaRepository<Truck, UUID> {
    Optional<Truck> findByPlate(String plate);
    boolean existsByPlate(String plate);
}
