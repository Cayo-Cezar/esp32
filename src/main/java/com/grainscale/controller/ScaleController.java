package com.grainscale.controller;

import com.grainscale.dto.CreateScaleRequest;
import com.grainscale.model.Scale;
import com.grainscale.service.ScaleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/scales")
public class ScaleController {

    private final ScaleService service;

    public ScaleController(ScaleService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Scale> create(@Valid @RequestBody CreateScaleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public Scale findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/external/{externalId}")
    public Scale findByExternalId(@PathVariable String externalId) {
        return service.findByExternalId(externalId);
    }

    @GetMapping
    public List<Scale> findAll() {
        return service.findAll();
    }
}
