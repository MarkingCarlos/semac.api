package com.semac.java_api.dto;

public record OrcamentoResponseDTO(
        Integer id,
        Integer ano,
        Integer inscritosPrevistos,
        Integer membrosComissao,
        Integer palestrantesPrevistos
) {}
