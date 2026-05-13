package com.grainscale.repository;

import com.grainscale.model.Grain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GrainRepository extends JpaRepository<Grain, UUID> {
    boolean existsByName(String name);
}
