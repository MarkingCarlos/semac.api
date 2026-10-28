package com.semac.java_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/* Envio do quiz de um laboratório (POST /api/laboratorio/quiz/<codigo>/resposta):
   uma escolha por pergunta. Quem responde sai do token. */
public record RespostaQuizLaboratorioRequestDTO(
        @NotNull @Size(max = 30) List<@Valid @NotNull Escolha> escolhas
) {
    public record Escolha(@NotNull Integer perguntaId, @NotNull Integer alternativaId) {}
}
