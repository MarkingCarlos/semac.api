package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/* A prévia renderiza o que está na tela, não o que está salvo — é o que
   permite ver o resultado antes de gravar. */
public record PreviaEmailRequestDTO(
        @NotBlank @Size(max = 200) String assunto,
        @NotBlank String corpoMarkdown
) {}
