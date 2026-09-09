package com.semac.java_api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/* `camisetasGratis` é quantas camisetas o ingresso inclui (0 = nenhuma).
   `porDia` marca ingresso de diária: `valor` passa a ser o preço de um dia
   e `maxDias` limita quantos dias podem ser escolhidos no cadastro.
   `codigo`/`alterarCodigo`: o código nunca volta numa resposta da API
   (ver TipoInscricaoResponseDTO), então o front não tem como reenviar o
   valor atual ao editar outro campo. `alterarCodigo=true` é o sinal
   explícito de que `codigo` deve ser aplicado (branco = remove a
   exigência); `false` preserva o que já estava salvo.
   `restritoUnesp` esconde o ingresso pra quem não marcou "Sou da UNESP" no
   cadastro público (ver TipoInscricaoController e InscricaoService). */
public record TipoInscricaoRequestDTO(
        @NotBlank String nome,
        @NotNull @PositiveOrZero BigDecimal valor,
        @NotNull Integer ano,
        Boolean ativo,
        @PositiveOrZero Integer camisetasGratis,
        Boolean porDia,
        @Min(1) Integer maxDias,
        String codigo,
        Boolean alterarCodigo,
        Boolean restritoUnesp
) {}
