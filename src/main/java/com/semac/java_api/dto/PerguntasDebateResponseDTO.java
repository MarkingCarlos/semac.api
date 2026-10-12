package com.semac.java_api.dto;

/* Se os participantes podem enviar perguntas para os debates na edição.
   Ano sem configuração cadastrada volta fechado (false). */
public record PerguntasDebateResponseDTO(
        Integer ano,
        Boolean perguntasDebateAbertas
) {}
