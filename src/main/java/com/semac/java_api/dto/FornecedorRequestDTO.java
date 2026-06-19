package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;

public record FornecedorRequestDTO(
        @NotBlank String nome,
        String contato,
        String observacao
) {}
