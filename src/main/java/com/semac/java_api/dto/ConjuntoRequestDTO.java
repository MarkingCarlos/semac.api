package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;

/* Variações são geridas à parte (/api/variacao) — o conjunto só tem nome. */
public record ConjuntoRequestDTO(
        @NotBlank String nome
) {}
