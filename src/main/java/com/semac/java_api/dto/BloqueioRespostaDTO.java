package com.semac.java_api.dto;

/* Corpo do 429 de bloqueio por tentativas: além da `mensagem` de sempre,
   quanto falta para poder tentar de novo. */
public record BloqueioRespostaDTO(String mensagem, long segundosRestantes) {}
