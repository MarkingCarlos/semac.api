package com.semac.java_api.dto;

/* Uma tentativa já gasta, como o jogo a redesenha na grade.

   `resultado` é o padrão de cores compactado (C/P/A, uma letra por
   posição). O palpite volta porque foi o próprio jogador que o digitou —
   a palavra secreta é que não aparece. */
public record TermoTentativaDTO(
        String palpite,
        String resultado
) {}
