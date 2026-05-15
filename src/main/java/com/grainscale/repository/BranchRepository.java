package com.grainscale.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.grainscale.model.Branch;

public interface BranchRepository extends JpaRepository<Branch, UUID> {
    boolean existsByName(String name);
}