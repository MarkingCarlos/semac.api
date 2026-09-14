package com.semac.java_api.dto;

import java.math.BigDecimal;
import java.util.List;

/* Consolidado que alimenta o dashboard da aba Previsão.

   A distinção que a planilha de origem não fazia:
   - previstoAberto: itens ainda não pagos (o que falta gastar)
   - realizado:      compras registradas (o que já saiu)
   - projecaoTotal:  a soma dos dois, que é o número a comparar com o teto

   `teto` é o que a comissão arrecadou — patrocínios recebidos, doações e
   inscrições. Não se digita e não desconta as compras: elas já entram na
   projeção, e descontá-las aqui as contaria duas vezes.

   `reservaFundunesp` é o saldo de emergência, exibido à parte justamente
   por não entrar em nada. */
public record PrevisaoResumoDTO(
        BigDecimal previstoAberto,
        BigDecimal realizado,
        BigDecimal projecaoTotal,
        BigDecimal teto,
        BigDecimal margem,
        EntradasDTO entradas,
        BigDecimal reservaFundunesp,
        List<PrevisaoCategoriaResponseDTO> categorias,
        OrcamentoResponseDTO orcamento
) {
    /* As três fontes que compõem o teto, abertas para a interface poder
       mostrar de onde o número vem. */
    public record EntradasDTO(
            BigDecimal patrocinios,
            BigDecimal doacoes,
            BigDecimal inscricoes,
            BigDecimal total
    ) {}
}
