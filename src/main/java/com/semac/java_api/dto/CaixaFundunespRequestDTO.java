package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/* A data e o autor da alteração são definidos pelo backend — nunca
   enviados pelo cliente. */
public record CaixaFundunespRequestDTO(
        @NotNull @PositiveOrZero BigDecimal valor
) {}
