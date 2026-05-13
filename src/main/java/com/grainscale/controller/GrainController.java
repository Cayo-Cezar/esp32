package com.grainscale.controller;

import com.grainscale.dto.CreateGrainRequest;
import com.grainscale.model.Grain;
import com.grainscale.service.GrainService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/grains")
public class GrainController {

    private final GrainService service;

    public GrainController(GrainService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Grain> create(@Valid @RequestBody CreateGrainRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public Grain findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping
    public List<Grain> findAll() {
        return service.findAll();
    }
}
