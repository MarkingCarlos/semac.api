package com.semac.java_api.dto;

import java.math.BigDecimal;

/* `codigoDefinido` diz se este ingresso exige código no cadastro público —
   o valor do código em si nunca é exposto por essa API. `restritoUnesp`
   diz se o ingresso só aparece pra quem marcou "Sou da UNESP". */
public record TipoInscricaoResponseDTO(
        Integer id,
        String nome,
        BigDecimal valor,
        Integer ano,
        Boolean ativo,
        Integer camisetasGratis,
        Boolean porDia,
        Integer maxDias,
        Boolean codigoDefinido,
        Boolean restritoUnesp
) {}
