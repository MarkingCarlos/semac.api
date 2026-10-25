package com.semac.java_api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/* Põe a pergunta num bloco do roteiro do debate ou tira (bloco null)
   — PATCH /api/perguntas/{id}/bloco. O painel usa 4 blocos; o teto aqui
   é folgado para um debate maior não exigir migration. */
public record PerguntaBlocoRequestDTO(
        @Min(value = 1, message = "Bloco inválido.")
        @Max(value = MAXIMO_BLOCOS, message = "Bloco inválido.")
        Integer bloco
) {
    public static final int MAXIMO_BLOCOS = 10;
}
