package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record OrcamentoRequestDTO(
        @NotNull @PositiveOrZero BigDecimal teto,
        @NotNull @PositiveOrZero Integer inscritosPrevistos,
        @NotNull @PositiveOrZero Integer membrosComissao,
        @NotNull @PositiveOrZero Integer palestrantesPrevistos
) {}
