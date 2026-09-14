package com.semac.java_api.dto;

import java.math.BigDecimal;

/* `totalPrevisto` e `totalRealizado` são calculados na leitura, com as
   escalas já aplicadas — ver PrevisaoService. */
public record PrevisaoCategoriaResponseDTO(
        Integer id,
        String nome,
        String cor,
        BigDecimal teto,
        Integer ordem,
        BigDecimal totalPrevisto,
        BigDecimal totalRealizado
) {}
