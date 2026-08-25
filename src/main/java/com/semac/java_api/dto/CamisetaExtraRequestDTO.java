package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CamisetaExtraRequestDTO(
        @NotNull Integer ano,
        @NotNull @PositiveOrZero BigDecimal valor
) {}
