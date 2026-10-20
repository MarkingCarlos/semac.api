package com.semac.java_api.dto;

/* O que a tela de teste do /admin -> Termo precisa para montar a grade:
   só o tamanho da palavra. O texto dela não vem: a graça do teste é jogar
   sem saber a resposta. */
public record TermoTesteDTO(
        int tamanho
) {}
