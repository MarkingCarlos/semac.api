package com.semac.java_api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/* Edição de uma pergunta da mediação (PUT /api/perguntas/{id}): texto e
   bloco. Pergunta do público não é editável — só escolhida para um bloco. */
public record PerguntaMediacaoRequestDTO(
        @NotBlank(message = "Escreva a pergunta.")
        @Size(max = 500, message = "A pergunta pode ter no máximo 500 caracteres.")
        String texto,

        @Min(value = 1, message = "Bloco inválido.")
        @Max(value = PerguntaBlocoRequestDTO.MAXIMO_BLOCOS, message = "Bloco inválido.")
        Integer bloco
) {}
