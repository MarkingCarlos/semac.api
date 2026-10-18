package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/* Palpite do modo teste do /admin -> Termo: a diretoria escolhe contra
   qual palavra cadastrada joga. */
public record TermoPalpiteTesteDTO(
        @NotNull Integer palavraId,
        @NotBlank String palpite
) {}
