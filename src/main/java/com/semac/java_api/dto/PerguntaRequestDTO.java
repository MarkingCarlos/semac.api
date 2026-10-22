package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/* Envio de pergunta para um debate (POST /api/perguntas). `nome` é
   opcional e só vale sem login — com login, o nome é o da conta. */
public record PerguntaRequestDTO(
        @NotNull(message = "Escolha o debate.")
        Integer eventoId,

        @NotBlank(message = "Escreva sua pergunta.")
        @Size(max = 500, message = "A pergunta pode ter no máximo 500 caracteres.")
        String texto,

        @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres.")
        String nome
) {}
