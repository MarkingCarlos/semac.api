package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PrevisaoCategoriaRequestDTO(
        @NotBlank @Size(max = 80) String nome,
        @NotBlank @Size(max = 20) String cor,
        /* Null = categoria sem teto próprio. */
        @PositiveOrZero BigDecimal teto,
        Integer ordem
) {}
