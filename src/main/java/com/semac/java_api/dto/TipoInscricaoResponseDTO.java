package com.semac.java_api.dto;

import java.math.BigDecimal;

public record TipoInscricaoResponseDTO(
        Integer id,
        String nome,
        BigDecimal valor,
        Integer ano,
        Boolean ativo,
        Integer camisetasGratis,
        Boolean porDia,
        Integer maxDias
) {}
