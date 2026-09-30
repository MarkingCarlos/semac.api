package com.semac.java_api.dto;

import java.time.LocalDateTime;

/* Uma leitura de QR: o membro `operadorNome` leu o crachá do participante
   `participanteNome` em `lidoEm`. Operador nulo = check-in anterior à V47. */
public record DashboardLeituraQrDTO(
        Integer participanteId,
        String participanteNome,
        Integer operadorId,
        String operadorNome,
        LocalDateTime lidoEm
) {}
