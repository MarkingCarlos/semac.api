package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/* valorTotal e dataCompra não vêm do cliente: o backend calcula
   valorTotal (valorUnitario × quantidade) e define dataCompra. */
public record CompraRequestDTO(
        @NotBlank String descricao,
        @NotBlank String categoria,
        @NotNull Integer fornecedorId,
        @NotNull @PositiveOrZero BigDecimal valorUnitario,
        @NotNull @Positive Integer quantidade
) {}
