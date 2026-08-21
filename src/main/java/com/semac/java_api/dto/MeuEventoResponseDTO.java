package com.semac.java_api.dto;

/* Um evento em que o participante autenticado está, com o status dele
   ali (INSCRITO | PRESENTE | AUSENTE). O evento vem completo para a área
   do participante montar a agenda sem cruzar listas. */
public record MeuEventoResponseDTO(
        EventoResponseDTO evento,
        String status
) {}
