package com.grainscale.service;

import com.grainscale.dto.CreateGrainRequest;
import com.grainscale.exception.ResourceNotFoundException;
import com.grainscale.model.Grain;
import com.grainscale.repository.GrainRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GrainService {

    private final GrainRepository repository;

    public GrainService(GrainRepository repository) {
        this.repository = repository;
    }

    public Grain create(CreateGrainRequest request) {
        if (repository.existsByName(request.name())) {
            throw new IllegalArgumentException("Já existe um grão com o nome: " + request.name());
        }
        var grain = new Grain(request.name(), request.purchasePricePerTon());
        return repository.save(grain);
    }

    public Grain findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grão não encontrado: " + id));
    }

    public List<Grain> findAll() {
        return repository.findAll();
    }
}
