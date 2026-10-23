package com.semac.java_api.dto;

/* Resultado de uma tentativa: se acertou a etapa e o estado como ficou,
   para a tela se redesenhar sem outra ida ao servidor. */
public record DesafioCifraTentativaRespostaDTO(
        boolean acertou,
        DesafioCifraEstadoDTO estado
) {}
