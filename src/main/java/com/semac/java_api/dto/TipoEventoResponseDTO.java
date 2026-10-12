package com.semac.java_api.dto;

public record TipoEventoResponseDTO(
        Integer id,
        String nome,
        Integer pontos,
        Boolean exigeInscricao,
        /* Chave estável do tipo (CodigoTipoEvento) — é por ela que o front
           reconhece um debate, nunca pelo nome editável. */
        String codigo
) {}
