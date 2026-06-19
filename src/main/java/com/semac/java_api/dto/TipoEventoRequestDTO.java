package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record TipoEventoRequestDTO(
        @NotBlank String nome,
        @NotNull @PositiveOrZero Integer pontos
) {}
