package com.grainscale.controller;

import com.grainscale.dto.WeighingRequest;
import com.grainscale.service.WeighingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint de ingestão de dados do ESP32.
 *
 * O ESP32 envia POST a cada 100ms com o peso atual.
 * Retorna 202 Accepted imediatamente (fire-and-forget).
 */
@RestController
@RequestMapping("/api/weighing")
public class WeighingController {

    private final WeighingService weighingService;

    public WeighingController(WeighingService weighingService) {
        this.weighingService = weighingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receiveReading(@Valid @RequestBody WeighingRequest request) {
        weighingService.processReading(request);
    }
}
