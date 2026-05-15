package com.grainscale.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.grainscale.dto.CreateBranchRequest;
import com.grainscale.exception.ResourceNotFoundException;
import com.grainscale.model.Branch;
import com.grainscale.repository.BranchRepository;

@Service
public class BranchService {

    private final BranchRepository repository;

    public BranchService(BranchRepository repository) {
        this.repository = repository;
    }

    public Branch create(CreateBranchRequest request) {
        if (repository.existsByName(request.name())) {
            throw new IllegalArgumentException("Já existe uma filial com o nome: " + request.name());
        }
        var branch = new Branch(request.name(), request.location());
        return repository.save(branch);
    }

    public Branch findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Filial não encontrada: " + id));
    }

    public List<Branch> findAll() {
        return repository.findAll();
    }
}