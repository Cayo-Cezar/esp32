package com.grainscale.controller;

import com.grainscale.model.Transaction;
import com.grainscale.repository.TransactionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository repository;

    public TransactionController(TransactionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Transaction> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Transaction findById(@PathVariable UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new com.grainscale.exception.ResourceNotFoundException(
                        "Transação não encontrada: " + id));
    }

    @GetMapping("/grain/{grainId}")
    public List<Transaction> findByGrain(@PathVariable UUID grainId) {
        return repository.findByGrainId(grainId);
    }
}
