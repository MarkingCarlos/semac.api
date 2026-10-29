package com.semac.java_api.dto;

/* Se o ranking de XP (participantes e times) está oculto na edição.
   Ano sem configuração cadastrada volta visível (false). */
public record RankingOcultoResponseDTO(
        Integer ano,
        Boolean rankingOculto
) {}
