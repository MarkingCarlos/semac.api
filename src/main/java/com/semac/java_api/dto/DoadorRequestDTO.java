package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DoadorRequestDTO(
        @NotBlank String nome,
        @NotNull @PositiveOrZero BigDecimal valor,
        @NotNull LocalDateTime data
) {}
