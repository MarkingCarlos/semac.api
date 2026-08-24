package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BrindeRequestDTO(
        @NotBlank String nome,
        @NotNull @Positive Integer quantidade
) {}
