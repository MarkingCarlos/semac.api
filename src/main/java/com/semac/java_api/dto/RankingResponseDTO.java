package com.semac.java_api.dto;

import java.util.List;

/* Resposta de GET /api/pessoa/ranking — lista completa de participantes com
   XP atribuído, ordenada do maior pro menor. O front fatia pódio/vizinhança
   do usuário a partir dela; aqui não há paginação (evento pequeno o bastante
   pra devolver tudo de uma vez). */
public record RankingResponseDTO(
        List<RankingParticipanteDTO> lista,
        int totalParticipantes,
        String atualizadoEm
) {}
