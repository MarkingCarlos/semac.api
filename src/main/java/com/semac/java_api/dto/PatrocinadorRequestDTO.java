package com.semac.java_api.dto;

import com.semac.java_api.model.enums.StatusPagamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/* valorFinal não vem do cliente: o backend calcula a partir da cota
   (cota.valor − desconto + adicao). */
public record PatrocinadorRequestDTO(
        @NotBlank String nome,
        String descricao,
        String logoUrl,
        @NotNull Integer cotaId,
        BigDecimal desconto,
        BigDecimal adicao,
        StatusPagamento statusPagamento,
        LocalDateTime dataRecebimento,
        String observacao
) {}
