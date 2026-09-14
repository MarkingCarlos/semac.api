package com.semac.java_api.dto;

/* `inscritosPrevistos` é derivado (contagem de pessoas com role
   PARTICIPANTE ou NULL) e vem só de leitura — não é aceito no PUT. */
public record OrcamentoResponseDTO(
        Integer id,
        Integer ano,
        Integer inscritosPrevistos,
        Integer membrosComissao,
        Integer palestrantesPrevistos
) {}
