package com.semac.java_api.dto;

import java.math.BigDecimal;

/* Inscrição que entra no saldo da comissão (somente leitura).

   Uma por pessoa com ingresso definido, confirmada ou não: o dinheiro de
   quem já pagou está na conta independentemente de o organizador ter
   chegado na fila de confirmação. `confirmada` diz em qual dos dois
   grupos a linha está (ver PessoaService.listarInscricoes).

   Três valores em vez de um porque o cartão não entrega o que cobra:
   - valorBruto:   ingresso × diárias, o que a pessoa pagou
   - taxaCartao:   o que a maquininha reteve (zero fora do cartão)
   - valorLiquido: o que sobrou para a comissão — é este que entra no
                   saldo e no teto da previsão.

   Camiseta avulsa fica de fora dos três: o cartão a cobra junto, mas o
   ganho com ela entra no saldo à parte, como lucro (receita − custo) — ver
   PrevisaoService.resumo. */
public record InscricaoFinanceiraDTO(
        Integer id,
        String nomePessoa,
        String tipoInscricao,
        BigDecimal valorBruto,
        BigDecimal taxaCartao,
        BigDecimal valorLiquido,
        String formaPagamento,
        boolean confirmada,
        Integer ano
) {}
