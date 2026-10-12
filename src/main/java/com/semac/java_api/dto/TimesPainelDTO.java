package com.semac.java_api.dto;

import java.util.List;

/* Tudo que a aba Times do /admin precisa numa chamada só: o interruptor
   da revelação, os três times e os totais usados no aviso do botão
   "Sortear" (refazer com gente já revelada perde o que eles viram). */
public record TimesPainelDTO(
        boolean ativos,
        long participantesElegiveis,
        long sorteados,
        long revelados,
        List<TimeAdminDTO> times
) {}
