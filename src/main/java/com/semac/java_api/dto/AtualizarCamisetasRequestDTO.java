package com.semac.java_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/* Corpo do PUT /api/pessoa/{id}/camisetas — substitui a lista inteira de
   camisetas da pessoa pela enviada aqui (replace-all). Lista vazia é válida:
   significa que a pessoa fica sem nenhuma camiseta pedida. */
public record AtualizarCamisetasRequestDTO(
        @NotNull @Valid List<CamisetaAdminDTO> camisetas
) {}
