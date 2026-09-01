package com.semac.java_api.dto;

/* Quantidade a comprar de uma combinação modelo+tamanho. */
public record ItemEstoqueCamisetaDTO(
        String modelo,
        String tamanho,
        long total
) {}
