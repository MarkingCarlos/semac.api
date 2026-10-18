package com.semac.java_api.dto;

/* Se uma palavra está no dicionário de palpites do Termo. É o que o modo
   treino do /termo consulta — ele confere o palpite no navegador, mas o
   dicionário (5 a 10 letras) é grande demais para ir no bundle. */
public record TermoPalavraValidaDTO(
        boolean valida
) {}
