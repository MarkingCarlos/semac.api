package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;

public record RankingOcultoRequestDTO(
        @NotNull Integer ano,
        @NotNull Boolean rankingOculto
) {}
