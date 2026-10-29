package com.semac.java_api.dto;

/* Placar da tentativa no quiz de um laboratório: só quantas acertou.
   Nem a correta nem o que a pessoa errou saem do servidor — quem
   respondeu não leva o gabarito para os próximos. */
public record ResultadoQuizLaboratorioDTO(
        int acertos,
        int total,
        boolean gabaritou,
        int xpCreditado
) {}
