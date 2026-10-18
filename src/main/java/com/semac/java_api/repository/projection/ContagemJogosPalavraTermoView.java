package com.semac.java_api.repository.projection;

/* Quantos jogos já foram sorteados para cada palavra do Termo. É o que o
   sorteio usa para escolher a palavra menos usada do dia, e o que o /admin
   mostra ao lado de cada uma. Palavra sem jogo nenhum não aparece. */
public interface ContagemJogosPalavraTermoView {
    Integer getPalavraId();
    Long getTotal();
}
