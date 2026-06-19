package com.semac.java_api.dto;

import java.math.BigDecimal;

/* Inscrição confirmada para o módulo financeiro (somente leitura).
   Uma por participante (role = PARTICIPANTE) com ingresso definido.
   `valor` vem do tipo de ingresso escolhido na confirmação. */
public record InscricaoFinanceiraDTO(
        Integer id,
        String nomePessoa,
        String tipoInscricao,
        BigDecimal valor,
        Integer ano
) {}
