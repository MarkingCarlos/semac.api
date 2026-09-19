package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModeloEmailRequestDTO(
        @NotBlank @Size(max = 200) String assunto,
        @NotBlank String corpoMarkdown,
        Boolean ativo
) {}
