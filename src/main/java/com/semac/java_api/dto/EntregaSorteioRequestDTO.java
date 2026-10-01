package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;

public record EntregaSorteioRequestDTO(
        @NotNull Integer brindeId,
        @NotNull Integer participanteId
) {}
