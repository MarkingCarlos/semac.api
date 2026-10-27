package com.semac.java_api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/* Nota da pesquisa de satisfação de um evento, de 0 a 10. */
public record AvaliacaoEventoRequestDTO(
        @NotNull(message = "Escolha uma nota de 0 a 10.")
        @Min(value = 0, message = "A nota vai de 0 a 10.")
        @Max(value = 10, message = "A nota vai de 0 a 10.")
        Integer nota
) {}
