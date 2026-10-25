package com.semac.java_api.dto;

import java.time.LocalDateTime;
import java.util.List;

/* Tudo que a aba Times do /admin precisa numa chamada só: o interruptor
   da revelação, os três times e os totais usados no aviso do botão
   "Sortear" (refazer com gente já revelada perde o que eles viram).

   `ativos` é só o interruptor; `revelacaoProgramadaEm` o horário da
   revelação automática (null = sem horário); `liberados` diz se os times
   valem agora, por um ou pelo outro. */
public record TimesPainelDTO(
        boolean ativos,
        LocalDateTime revelacaoProgramadaEm,
        boolean liberados,
        long participantesElegiveis,
        long sorteados,
        long revelados,
        List<TimeAdminDTO> times
) {}
