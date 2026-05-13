package com.grainscale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateGrainRequest(
        @NotBlank(message = "O nome do grão é obrigatório")
        String name,

        @NotNull(message = "O preço de compra é obrigatório")
        @Positive(message = "O preço deve ser positivo")
        BigDecimal purchasePricePerTon
) {}
