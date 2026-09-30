package com.semac.java_api.dto;

/* Quantos QR um membro da comissão leu. `operadorId` nulo agrupa os
   check-ins feitos antes da V47, quando o operador não era gravado. */
public record DashboardLeiturasMembroDTO(
        Integer operadorId,
        String operadorNome,
        long leituras
) {}
