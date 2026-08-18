package com.semac.java_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CotacaoRequestDTO(
        @NotBlank String descricao,
        @NotBlank String categoria,
        @NotNull @Positive Integer quantidade,
        @NotEmpty(message = "Informe ao menos um fornecedor.") @Valid List<CotacaoFornecedorRequestDTO> fornecedores
) {}
