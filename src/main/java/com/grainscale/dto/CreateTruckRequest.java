package com.grainscale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreateTruckRequest(
        @NotBlank(message = "A placa é obrigatória")
        String plate,

        @NotNull(message = "O peso da tara é obrigatório")
        @Positive(message = "O peso da tara deve ser positivo")
        Double tareWeight,

        @NotNull(message = "O ID do grão é obrigatório")
        UUID grainId
) {}
