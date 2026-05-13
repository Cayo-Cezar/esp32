package com.grainscale.controller;

import com.grainscale.dto.CreateBranchRequest;
import com.grainscale.model.Branch;
import com.grainscale.service.BranchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService service;

    public BranchController(BranchService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Branch> create(@Valid @RequestBody CreateBranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public Branch findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping
    public List<Branch> findAll() {
        return service.findAll();
    }
}
