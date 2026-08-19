package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record NivelRequestDTO(
        @NotBlank String nome,
        @NotNull @PositiveOrZero Integer xpMinimo
) {}
