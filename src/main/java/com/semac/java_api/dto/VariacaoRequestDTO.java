package com.semac.java_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/* itens pode vir nulo/vazio (ex.: "Zerar todas as quantidades"). */
public record VariacaoRequestDTO(
        @NotBlank String nome,
        @Valid List<VariacaoItemRequestDTO> itens
) {}
