package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SorteioRequestDTO(
        @NotBlank String nome,
        @NotNull Integer eventoId
) {}
