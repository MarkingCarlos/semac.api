package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/* Palestrante de um evento (relação N:N `evento_palestrante`).
   `id` é null na criação; preenchido na resposta. */
public record PalestranteDTO(
        Integer id,
        @NotBlank String nome,
        @Size(max = 2000) String descricao
) {}
