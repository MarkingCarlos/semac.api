package com.semac.java_api.dto;

/* Participante que ocupa vaga num minicurso. `presente` diz se o QR dele
   já foi lido naquele minicurso. */
public record DashboardInscritoMinicursoDTO(
        Integer id,
        String nome,
        String email,
        boolean presente
) {}
