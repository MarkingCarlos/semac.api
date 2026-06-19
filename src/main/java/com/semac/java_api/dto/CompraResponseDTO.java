package com.semac.java_api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompraResponseDTO(
        Integer id,
        String descricao,
        String categoria,
        Integer fornecedorId,
        BigDecimal valorUnitario,
        Integer quantidade,
        BigDecimal valorTotal,
        LocalDateTime dataCompra
) {}
