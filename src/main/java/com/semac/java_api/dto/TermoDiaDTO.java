package com.semac.java_api.dto;

import java.time.LocalDate;

/* Resumo de um dia de Termo já ocorrido, do ponto de vista do participante
   logado — uma linha por card da aba "Desafios" em /participantes.

   Diferente de TermoEstadoDTO, aqui a palavra secreta não aparece em
   situação nenhuma, nem com o jogo encerrado: este resumo cobre também
   dias passados, e quem quisesse a resposta de ontem poderia lê-la para
   um dia que ainda não jogou. A tela de fim do /termo continua sendo a
   única porta por onde a palavra sai, e só para a partida da própria
   pessoa, já acabada.

   Também não vêm as tentativas em si (palpite + padrão de cores): o card
   só precisa de quantas foram gastas para dizer "em andamento".

   `encerrado` false com `tentativasUsadas` 0 = a pessoa não jogou esse
   dia. Dias futuros não entram na lista. */
public record TermoDiaDTO(
        Integer dia,
        LocalDate data,
        boolean hoje,
        boolean encerrado,
        boolean venceu,
        Integer xpGanho,
        int tentativasUsadas
) {}
