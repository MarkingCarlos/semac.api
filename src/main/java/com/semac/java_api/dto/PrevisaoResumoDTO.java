package com.semac.java_api.dto;

import java.math.BigDecimal;
import java.util.List;

/* Consolidado que alimenta o dashboard da aba Previsão.

   A distinção que a planilha de origem não fazia:
   - previstoAberto: itens ainda não pagos (o que falta gastar)
   - realizado:      compras registradas (o que já saiu)
   - projecaoTotal:  a soma dos dois, que é o número a comparar com o teto

   `teto` é o quanto se pode comprometer: o que a comissão arrecadou
   (`entradas.total` — patrocínios recebidos, doações e inscrições) MAIS
   `patrociniosAReceber`, os contratos assinados que ainda não foram
   pagos. Não se digita e não desconta as compras: elas já entram na
   projeção, e descontá-las aqui as contaria duas vezes.

   O patrocínio a receber entra só aqui, nunca em `entradas`: contrato
   assinado dá lastro para planejar um gasto, mas não é dinheiro que se
   possa sacar hoje. Por isso o teto é maior que a arrecadação, e os dois
   números aparecem separados na interface.

   `reservaFundunesp` é o saldo de emergência, exibido à parte justamente
   por não entrar em nada. */
public record PrevisaoResumoDTO(
        BigDecimal previstoAberto,
        BigDecimal realizado,
        BigDecimal projecaoTotal,
        BigDecimal teto,
        BigDecimal margem,
        BigDecimal patrociniosAReceber,
        EntradasDTO entradas,
        BigDecimal reservaFundunesp,
        List<PrevisaoCategoriaResponseDTO> categorias,
        OrcamentoResponseDTO orcamento
) {
    /* As três fontes do dinheiro que a comissão já tem, abertas para a
       interface poder mostrar de onde o número vem. Patrocínio a receber
       não entra em nenhuma delas — está no `patrociniosAReceber` do
       resumo, fora da arrecadação.

       `inscricoes` é a soma de `inscricoesConfirmadas` com
       `inscricoesPendentes` — as duas vêm separadas porque pendente com
       pagamento evidenciado entra no saldo (ver
       PessoaService.listarInscricoes) e o Resumo mostra as duas parcelas.
       Todas já líquidas da taxa do cartão. */
    public record EntradasDTO(
            BigDecimal patrocinios,
            BigDecimal doacoes,
            BigDecimal inscricoes,
            BigDecimal inscricoesConfirmadas,
            BigDecimal inscricoesPendentes,
            BigDecimal total
    ) {}
}
