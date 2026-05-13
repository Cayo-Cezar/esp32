package com.grainscale.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBranchRequest(
        @NotBlank(message = "O nome da filial é obrigatório")
        String name,

        @NotBlank(message = "A localização é obrigatória")
        String location
) {}
