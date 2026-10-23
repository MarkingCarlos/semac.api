package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DesafioCifraTentativaDTO(
        @NotBlank @Size(max = 100) String resposta
) {}
