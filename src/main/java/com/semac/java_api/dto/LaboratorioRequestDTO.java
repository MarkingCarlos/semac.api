package com.semac.java_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/* Cadastro ou edição de um laboratório no /admin, já com as perguntas: a
   edição troca a lista inteira. O código do QR não entra — é gerado pelo
   servidor e não muda, senão o QR já impresso pararia de funcionar.

   "Exatamente uma alternativa correta" é conferido no
   LaboratorioQuizService (a anotação não expressa isso). */
public record LaboratorioRequestDTO(
        @NotNull Integer ano,
        @NotBlank @Size(max = 200) String nome,
        @Size(max = 300) String responsaveis,
        @NotNull @Size(max = 30) List<@Valid @NotNull Pergunta> perguntas
) {
    public record Pergunta(
            @NotBlank @Size(max = 500) String enunciado,
            @NotNull @Size(min = 2, max = 5) List<@Valid @NotNull Alternativa> alternativas
    ) {}

    public record Alternativa(
            @NotBlank @Size(max = 300) String texto,
            boolean correta
    ) {}
}
