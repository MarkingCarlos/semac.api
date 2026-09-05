package com.semac.java_api.dto;

/* Uma linha do ranking de XP (GET /api/pessoa/ranking). Não expõe id/email/cpf
   — só o necessário pra exibir. `voce` é calculado no servidor comparando com
   o id da claim do token, nunca por nome (evita colisão entre homônimos). */
public record RankingParticipanteDTO(
        Integer posicao,
        String nome,
        Integer xp,
        boolean voce
) {}
