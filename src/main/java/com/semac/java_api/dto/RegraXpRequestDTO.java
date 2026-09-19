package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/* Edição de uma regra de xp: só nome e valor. A chave vem na URL e a
   unidade é fixa por regra — nenhuma das duas se edita. */
public record RegraXpRequestDTO(
        @NotBlank String nome,
        @NotNull @PositiveOrZero Integer valor
) {}
