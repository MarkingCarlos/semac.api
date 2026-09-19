package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/* `eventoId` só é usado quando `publico` = INSCRITOS_EM_EVENTO. */
public record ComunicadoRequestDTO(
        @NotBlank @Size(max = 200) String assunto,
        @NotBlank String corpoMarkdown,
        @NotBlank String publico,
        Integer eventoId
) {}
