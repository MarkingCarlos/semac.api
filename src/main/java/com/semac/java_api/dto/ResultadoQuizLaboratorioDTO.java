package com.semac.java_api.dto;

import java.util.List;

/* Placar da tentativa no quiz de um laboratório.

   `correcao` só vem logo depois do envio (POST .../resposta): para cada
   pergunta, a alternativa escolhida e se acertou. A correta nunca sai do
   servidor — quem errou não leva o gabarito para os próximos. Ao reabrir
   o QR depois, vem null — a tentativa guarda só o placar. */
public record ResultadoQuizLaboratorioDTO(
        int acertos,
        int total,
        boolean gabaritou,
        int xpCreditado,
        List<Correcao> correcao
) {
    public record Correcao(Integer perguntaId, Integer alternativaEscolhidaId, boolean acertou) {}
}
