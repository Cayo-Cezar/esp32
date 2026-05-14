package com.grainscale.service;

import com.grainscale.dto.CreateTruckRequest;
import com.grainscale.exception.ResourceNotFoundException;
import com.grainscale.model.Truck;
import com.grainscale.repository.TruckRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TruckService {

    private final TruckRepository repository;

    public TruckService(TruckRepository repository) {
        this.repository = repository;
    }

    public Truck create(CreateTruckRequest request) {
        if (repository.existsByPlate(request.plate())) {
            throw new IllegalArgumentException("Já existe um caminhão com a placa: " + request.plate());
        }
        var truck = new Truck(request.plate().toUpperCase(), request.tareWeight(), request.grainId());
        return repository.save(truck);
    }

    public Truck findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caminhão não encontrado: " + id));
    }

    public Truck findByPlate(String plate) {
        return repository.findByPlate(plate.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Caminhão não encontrado com placa: " + plate));
    }

    public List<Truck> findAll() {
        return repository.findAll();
    }
}
