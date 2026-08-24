package com.semac.java_api.dto;

import java.math.BigDecimal;

public record CotacaoFornecedorResponseDTO(
        Integer id,
        Integer fornecedorId,
        BigDecimal valorUnitario,
        BigDecimal frete
) {}
