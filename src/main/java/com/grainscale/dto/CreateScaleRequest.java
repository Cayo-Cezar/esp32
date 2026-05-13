package com.grainscale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateScaleRequest(
        @NotBlank(message = "O ID externo (ESP32) é obrigatório")
        String externalId,

        @NotNull(message = "O ID da filial é obrigatório")
        UUID branchId
) {}
