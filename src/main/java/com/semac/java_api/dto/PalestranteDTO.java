package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;

/* Palestrante de um evento (relação N:N `evento_palestrante`).
   `id` é null na criação; preenchido na resposta. */
public record PalestranteDTO(
        Integer id,
        @NotBlank String nome,
        String descricao
) {}
