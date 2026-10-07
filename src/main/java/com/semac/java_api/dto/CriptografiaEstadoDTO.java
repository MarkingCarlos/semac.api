package com.semac.java_api.dto;

/* Estado do jogo para o participante logado — o que a tela precisa
   para se montar, inclusive retomando uma partida deixada pela metade.

   `disponivel` false = hoje não é o dia da Criptografia. */
public record CriptografiaEstadoDTO(
        boolean disponivel,
        boolean venceu,
        Integer xpGanho,
        Integer acertos
) {}
