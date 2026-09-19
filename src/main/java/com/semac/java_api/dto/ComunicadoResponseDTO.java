package com.semac.java_api.dto;

public record ComunicadoResponseDTO(
        Integer id,
        String assunto,
        String publico,
        String rotuloPublico,
        String nomeEvento,
        Integer totalDestinatarios,
        Integer enviados,
        Integer falhas,
        String status,
        String criadoEm,
        String enviadoPor
) {}
