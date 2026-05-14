package com.grainscale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload JSON enviado pelo ESP32 a cada 100ms.
 * Exemplo: { "id": "ESP32-DOCK-01", "plate": "ABC1D23", "weight": 25430.5 }
 */
public record WeighingRequest(
        @NotBlank(message = "O ID da balança é obrigatório")
        String id,

        @NotBlank(message = "A placa é obrigatória")
        String plate,

        @NotNull(message = "O peso é obrigatório")
        @Positive(message = "O peso deve ser positivo")
        Double weight
) {}
