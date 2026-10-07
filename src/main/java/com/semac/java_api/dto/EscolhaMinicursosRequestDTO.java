package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;

public record EscolhaMinicursosRequestDTO(
        @NotNull Integer ano,
        @NotNull Boolean escolhaMinicursosAberta
) {}
