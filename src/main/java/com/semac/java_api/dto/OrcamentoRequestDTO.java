package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/* Sem teto: ele é derivado do saldo da conta da comissão, não digitado. */
public record OrcamentoRequestDTO(
        @NotNull @PositiveOrZero Integer membrosComissao,
        @NotNull @PositiveOrZero Integer palestrantesPrevistos
) {}
