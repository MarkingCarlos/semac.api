package com.semac.java_api.dto;

public record BrindeResponseDTO(
        Integer id,
        String nome,
        Integer quantidade,
        Integer quantidadeEntregue
) {}
