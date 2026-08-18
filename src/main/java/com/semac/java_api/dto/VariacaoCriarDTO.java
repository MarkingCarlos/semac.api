package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VariacaoCriarDTO(
        @NotNull Integer conjuntoId,
        @NotBlank String nome
) {}
