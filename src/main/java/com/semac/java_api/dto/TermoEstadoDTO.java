package com.semac.java_api.dto;

import java.util.List;

/* Estado do jogo de hoje para o participante logado — o que a tela precisa
   para se montar, inclusive retomando uma partida deixada pela metade.

   `palavra` vem null enquanto o jogo está em andamento e só é preenchida
   quando `encerrado` é true: aí a partida acabou e a tela de fim revela a
   resposta. É a única situação em que a palavra secreta sai da API.

   `tamanho` é quantas letras tem a palavra sorteada para a pessoa (5 a
   10) — a grade precisa disso para se desenhar, e é tudo o que sai dela.

   `disponivel` false = hoje não é dia de Termo (nenhum dia com palavra
   cadastrada para a data); nesse caso todo o resto vem vazio. */
public record TermoEstadoDTO(
        boolean disponivel,
        Integer dia,
        boolean ultimoDia,
        Integer tamanho,
        List<TermoTentativaDTO> tentativas,
        boolean encerrado,
        boolean venceu,
        Integer xpGanho,
        String palavra
) {}
