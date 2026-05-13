package com.grainscale.service;

import com.grainscale.dto.CreateBranchRequest;
import com.grainscale.exception.ResourceNotFoundException;
import com.grainscale.model.Branch;
import com.grainscale.repository.BranchRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BranchService {

    private final BranchRepository repository;

    public BranchService(BranchRepository repository) {
        this.repository = repository;
    }

    public Branch create(CreateBranchRequest request) {
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
