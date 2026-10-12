package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerguntaDebateRequestDTO(
        @NotBlank(message = "Escreva sua pergunta.")
        @Size(max = 500, message = "A pergunta pode ter no máximo 500 caracteres.")
        String texto
) {}
