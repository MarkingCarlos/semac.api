package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;

public record SorteioRequestDTO(
        @NotNull Integer eventoId,
        @NotNull Integer brindeId,
        @NotNull Integer participanteId
) {}
