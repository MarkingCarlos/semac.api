package com.semac.java_api.dto;

import com.semac.java_api.model.enums.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PatrocinadorResponseDTO(
        Integer id,
        String nome,
        String descricao,
        String logoUrl,
        CotaResponseDTO cota,
        BigDecimal desconto,
        BigDecimal adicao,
        BigDecimal valorFinal,
        StatusPagamento statusPagamento,
        LocalDateTime dataRecebimento,
        String observacao
) {}
