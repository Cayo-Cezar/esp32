package com.grainscale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTruckRequest(
        @NotBlank(message = "A placa é obrigatória")
        String plate,

        @NotNull(message = "O peso da tara é obrigatório")
        @Positive(message = "O peso da tara deve ser positivo")
        Double tareWeight
) {}
