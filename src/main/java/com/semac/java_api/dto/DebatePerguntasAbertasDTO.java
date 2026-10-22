package com.semac.java_api.dto;

import java.time.LocalDateTime;

/* Debate que está recebendo perguntas agora — opções da página pública
   /perguntas. Só o necessário para a pessoa reconhecer o debate. */
public record DebatePerguntasAbertasDTO(
        Integer id,
        String nome,
        LocalDateTime dataHoraInicio
) {}
