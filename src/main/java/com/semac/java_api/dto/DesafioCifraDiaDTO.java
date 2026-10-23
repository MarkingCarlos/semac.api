package com.semac.java_api.dto;

import java.time.LocalDate;

/* Resumo de um dia da Cifra já ocorrido, para os cards da aba "Desafios".
   Sem senha e sem resposta: é só o que a pessoa fez naquele dia.

   `encerrado` false com `tentativasUsadas` 0 = a pessoa não jogou. */
public record DesafioCifraDiaDTO(
        Integer id,
        LocalDate data,
        boolean hoje,
        boolean encerrado,
        boolean venceu,
        Integer xpGanho,
        int tentativasUsadas
) {}
