package com.semac.java_api.dto;

import java.math.BigDecimal;
import java.util.List;

/* Consolidado que alimenta o dashboard da aba Previsão.

   A distinção que a planilha de origem não fazia:
   - previstoAberto: itens ainda não pagos (o que falta gastar)
   - realizado:      compras registradas (o que já saiu)
   - projecaoTotal:  a soma dos dois, que é o número a comparar com o teto */
public record PrevisaoResumoDTO(
        BigDecimal previstoAberto,
        BigDecimal realizado,
        BigDecimal projecaoTotal,
        BigDecimal teto,
        BigDecimal margem,
        List<PrevisaoCategoriaResponseDTO> categorias,
        List<ContaResumoDTO> contas,
        OrcamentoResponseDTO orcamento
) {
    /* Balanço por conta — entradas e saídas da mesma conta, que é o
       cruzamento que a planilha fazia errado. */
    public record ContaResumoDTO(
            String conta,
            BigDecimal caixaInicial,
            BigDecimal entradas,
            BigDecimal saidas,
            BigDecimal previsto,
            BigDecimal saldo
    ) {}
}
