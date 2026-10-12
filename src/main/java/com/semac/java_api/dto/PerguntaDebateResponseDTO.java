package com.semac.java_api.dto;

import java.time.LocalDateTime;

/* Pergunta como o /admin vê: com o nome de quem enviou. O participante
   nunca recebe esta lista. */
public record PerguntaDebateResponseDTO(
        Integer id,
        String texto,
        String nomeParticipante,
        LocalDateTime enviadaEm
) {}
