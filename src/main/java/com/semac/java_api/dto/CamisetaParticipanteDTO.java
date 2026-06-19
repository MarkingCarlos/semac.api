package com.semac.java_api.dto;

/* Camiseta pedida na inscrição. `modelo` e `tamanho` são os nomes dos
   enums Modelo (NORMAL | BABY_LOOK) e Tamanho (PP | P | M | G | GG). */
public record CamisetaParticipanteDTO(String modelo, String tamanho) {}
