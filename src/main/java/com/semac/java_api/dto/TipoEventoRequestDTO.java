package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/* Corpo da edição de um tipo (PUT /api/tipo-evento/{id}). `exigeInscricao`
   é opcional: ausente vale como false (evento aberto). O `codigo` não
   entra — é do código, não do /admin. */
public record TipoEventoRequestDTO(
        @NotBlank String nome,
        @NotNull @PositiveOrZero Integer pontos,
        Boolean exigeInscricao
) {}
