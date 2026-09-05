package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;

public record ConfiguracaoInscricaoRequestDTO(
        @NotNull Integer ano,
        @NotNull Boolean inscricoesAbertas
) {}
