package com.semac.java_api.dto;

/* Uma linha do ranking completo da dashboard do /admin. Diferente do
   ranking da área do participante (RankingParticipanteDTO), aqui a
   comissão vê e-mail e nível para identificar a pessoa. */
public record DashboardRankingDTO(
        int posicao,
        Integer id,
        String nome,
        String email,
        int xp,
        String nivel
) {}
