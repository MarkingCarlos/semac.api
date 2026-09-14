package com.semac.java_api.dto;

import java.math.BigDecimal;

public record OrcamentoResponseDTO(
        Integer id,
        Integer ano,
        BigDecimal teto,
        Integer inscritosPrevistos,
        Integer membrosComissao,
        Integer palestrantesPrevistos
) {}
