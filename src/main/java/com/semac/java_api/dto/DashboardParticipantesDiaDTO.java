package com.semac.java_api.dto;

import java.time.LocalDate;

/* Um dia do card "Participantes por dia": passe completo + diaristas que
   escolheram este dia = total esperado no dia. */
public record DashboardParticipantesDiaDTO(
        LocalDate dia,
        int passeCompleto,
        int diaristas,
        int total
) {}
