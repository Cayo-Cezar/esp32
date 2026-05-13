package com.grainscale.repository;

import com.grainscale.model.Scale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScaleRepository extends JpaRepository<Scale, UUID> {
    Optional<Scale> findByExternalId(String externalId);
    boolean existsByExternalId(String externalId);
}
