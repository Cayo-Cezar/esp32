package com.grainscale.service;

import com.grainscale.dto.CreateScaleRequest;
import com.grainscale.exception.ResourceNotFoundException;
import com.grainscale.model.Scale;
import com.grainscale.repository.BranchRepository;
import com.grainscale.repository.ScaleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ScaleService {

    private final ScaleRepository scaleRepository;
    private final BranchRepository branchRepository;

    public ScaleService(ScaleRepository scaleRepository, BranchRepository branchRepository) {
        this.scaleRepository = scaleRepository;
        this.branchRepository = branchRepository;
    }

    public Scale create(CreateScaleRequest request) {
        if (scaleRepository.existsByExternalId(request.externalId())) {
            throw new IllegalArgumentException("Já existe uma balança com o ID: " + request.externalId());
        }

        // Verifica se a filial existe
        branchRepository.findById(request.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Filial não encontrada: " + request.branchId()));

        var scale = new Scale(request.externalId(), request.branchId());
        return scaleRepository.save(scale);
    }

    public Scale findById(UUID id) {
        return scaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Balança não encontrada: " + id));
    }

    public Scale findByExternalId(String externalId) {
        return scaleRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Balança não encontrada: " + externalId));
    }

    public List<Scale> findAll() {
        return scaleRepository.findAll();
    }
}
