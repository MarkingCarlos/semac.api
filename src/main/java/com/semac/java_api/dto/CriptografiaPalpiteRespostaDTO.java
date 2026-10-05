package com.semac.java_api.dto;

/* Resposta de um palpite: só a resposta se acertou ou não, o número de acertos
   e o xpGanho, nunca a palavra — esse é o ponto todo de a conferência ser no servidor. */
public record CriptografiaPalpiteRespostaDTO (
    boolean acertou,
    int acertos,
    Integer xpGanho
) {}
