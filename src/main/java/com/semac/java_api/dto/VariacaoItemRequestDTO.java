package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VariacaoItemRequestDTO(
        @NotNull Integer cotacaoId,
        @NotNull Integer fornecedorId,
        @NotNull @Positive Integer quantidade
) {}
