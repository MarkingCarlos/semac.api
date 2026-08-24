package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CotacaoFornecedorRequestDTO(
        @NotNull Integer fornecedorId,
        @NotNull @PositiveOrZero BigDecimal valorUnitario,
        @PositiveOrZero BigDecimal frete
) {
    /* Frete é opcional no corpo da requisição — ausente significa que o
       fornecedor não cobra. */
    public BigDecimal freteOuZero() {
        return frete == null ? BigDecimal.ZERO : frete;
    }
}
