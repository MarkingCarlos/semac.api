package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CotacaoFornecedorRequestDTO(
        @NotNull Integer fornecedorId,
        @NotNull @PositiveOrZero BigDecimal valorUnitario
) {}
