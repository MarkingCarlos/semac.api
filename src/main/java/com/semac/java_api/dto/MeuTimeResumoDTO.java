package com.semac.java_api.dto;

/* Resposta de GET /api/time/meu/resumo (bloco "Seu time" do perfil em
   /participantes). Só leitura: diferente de GET /api/time/meu, não revela
   nem encaixa ninguém em time.

   `revelado` false: times desligados, sorteio não feito ou a pessoa ainda
   não abriu /time — nenhum outro campo vem preenchido e o bloco não
   aparece. `posicao` é entre `totalTimes` (empatados dividem a posição);
   `mascoteVersao` é o cache-buster da URL pública do mascote, null quando
   o time não tem mascote. `xpTime` é a média de xp por integrante
   (arredondada), a mesma usada na posição. `rankingOculto` true: a
   organização escondeu o ranking no /admin — `posicao`, `totalTimes` e
   `xpTime` vêm zerados e o bloco mostra só o time. */
public record MeuTimeResumoDTO(
        boolean revelado,
        Integer id,
        String nome,
        String cor,
        String mascoteVersao,
        int posicao,
        int totalTimes,
        long xpTime,
        boolean rankingOculto
) {
    public static MeuTimeResumoDTO naoRevelado() {
        return new MeuTimeResumoDTO(false, null, null, null, null, 0, 0, 0, false);
    }
}
