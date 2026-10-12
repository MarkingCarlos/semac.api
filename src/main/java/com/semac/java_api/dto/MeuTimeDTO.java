package com.semac.java_api.dto;

/* Resposta de GET /api/time/meu (página /time).

   `ativo` false: a revelação está desligada no /admin e o front manda a
   pessoa para o /participantes — nenhum outro campo vem preenchido.
   `sorteado` false: ativo, mas o sorteio ainda não foi feito.
   `jaRevelado` true quando a pessoa já tinha aberto /time antes (a tela
   pula a animação de sorteio e diz "você já está no time").
   `mascoteVersao` é o nome do arquivo em disco, usado como cache-buster
   na URL pública do mascote; null quando o time não tem mascote. */
public record MeuTimeDTO(
        boolean ativo,
        boolean sorteado,
        boolean jaRevelado,
        Integer id,
        String nome,
        String cor,
        String mascoteVersao,
        long integrantes,
        int posicao,
        long xpTime
) {
    public static MeuTimeDTO desativado() {
        return new MeuTimeDTO(false, false, false, null, null, null, null, 0, 0, 0);
    }

    public static MeuTimeDTO aguardandoSorteio() {
        return new MeuTimeDTO(true, false, false, null, null, null, null, 0, 0, 0);
    }
}
