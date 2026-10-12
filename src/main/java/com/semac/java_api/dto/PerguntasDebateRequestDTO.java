package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;

public record PerguntasDebateRequestDTO(
        @NotNull Integer ano,
        @NotNull Boolean perguntasDebateAbertas
) {}
