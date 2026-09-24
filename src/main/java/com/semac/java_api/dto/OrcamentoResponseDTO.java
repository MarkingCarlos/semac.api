package com.semac.java_api.dto;

/* Todos os contadores são derivados e só de leitura.
   `inscritosPrevistos`: participantes + pendentes que ganham kit (sem
   ingresso diário) — escala POR_INSCRITO.
   `inscritosTotais`: participantes + pendentes, com diária — escala
   POR_INSCRITO_TOTAL. */
public record OrcamentoResponseDTO(
        Integer id,
        Integer ano,
        Integer inscritosPrevistos,
        Integer inscritosTotais,
        Integer membrosComissao,
        Integer palestrantesPrevistos
) {}
