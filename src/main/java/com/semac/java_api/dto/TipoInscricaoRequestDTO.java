package com.semac.java_api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/* `camisetasGratis` é quantas camisetas o ingresso inclui (0 = nenhuma).
   `porDia` marca ingresso de diária: `valor` passa a ser o preço de um dia
   e `maxDias` limita quantos dias podem ser escolhidos no cadastro. */
public record TipoInscricaoRequestDTO(
        @NotBlank String nome,
        @NotNull @PositiveOrZero BigDecimal valor,
        @NotNull Integer ano,
        Boolean ativo,
        @PositiveOrZero Integer camisetasGratis,
        Boolean porDia,
        @Min(1) Integer maxDias
) {}
