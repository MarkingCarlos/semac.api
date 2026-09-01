package com.semac.java_api.dto;

import java.math.BigDecimal;
import java.util.List;

/* Relatório de camisetas do /admin: quanto comprar no total, dividido
   entre dadas (inclusas no ingresso) e avulsas (compra extra), entre
   comissão e participantes, mais o financeiro da venda avulsa (receita ao
   preço de camiseta_extra, custo de produção e lucro) e o detalhamento por
   modelo/tamanho para orientar a compra real. */
public record RelatorioCamisetasDTO(
        int totalGeral,
        int totalDadas,
        int totalAvulsas,
        int totalComissao,
        int totalParticipantes,
        BigDecimal receitaAvulsas,
        BigDecimal custoAvulsas,
        BigDecimal lucroAvulsas,
        List<ItemEstoqueCamisetaDTO> porModeloTamanho
) {}
