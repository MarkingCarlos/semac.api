package com.semac.java_api.dto;

/* `total` vem nulo quando o público exige escolher um evento antes — a
   contagem só existe depois da escolha. */
public record PublicoComunicadoDTO(
        String publico,
        String rotulo,
        Boolean exigeEvento,
        Integer total
) {}
