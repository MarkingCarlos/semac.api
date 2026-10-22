package com.semac.java_api.dto;

import java.util.List;

/* Card "Participantes por dia" da dashboard: quantos participantes
   confirmados podem entrar em cada dia com programação.

   Quem tem passe completo conta em todos os dias; o diarista só nos dias
   que escolheu. totalPasseCompleto é a parte fixa de cada barra.
   diaristasSemEscolha: diaristas que ainda não escolheram nenhum dia e,
   por isso, não aparecem em barra nenhuma. */
public record DashboardParticipantesPorDiaDTO(
        int totalPasseCompleto,
        int diaristasSemEscolha,
        List<DashboardParticipantesDiaDTO> dias
) {}
