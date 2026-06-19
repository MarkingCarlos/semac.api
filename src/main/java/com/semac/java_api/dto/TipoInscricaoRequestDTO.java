package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record TipoInscricaoRequestDTO(
        @NotBlank String nome,
        @NotNull @PositiveOrZero BigDecimal valor,
        @NotNull Integer ano,
        Boolean ativo
) {}
