package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/* `exigeInscricao` é opcional no corpo: ausente vale como false (evento
   aberto), preservando os clientes que já criavam tipos sem o campo. */
public record TipoEventoRequestDTO(
        @NotBlank String nome,
        @NotNull @PositiveOrZero Integer pontos,
        Boolean exigeInscricao
) {}
