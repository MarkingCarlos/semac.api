package com.semac.java_api.dto;

/* Time em que o participante caiu no sorteio, para a página de resumo do /admin. */
public record TimeResumoParticipanteDTO(
        Integer id,
        String nome,
        String cor
) {}
