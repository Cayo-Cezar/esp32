package com.grainscale.controller;

import com.grainscale.dto.CreateTruckRequest;
import com.grainscale.model.Truck;
import com.grainscale.service.TruckService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trucks")
public class TruckController {

    private final TruckService service;

    public TruckController(TruckService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Truck> create(@Valid @RequestBody CreateTruckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public Truck findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/plate/{plate}")
    public Truck findByPlate(@PathVariable String plate) {
        return service.findByPlate(plate);
    }

    @GetMapping
    public List<Truck> findAll() {
        return service.findAll();
    }
}
