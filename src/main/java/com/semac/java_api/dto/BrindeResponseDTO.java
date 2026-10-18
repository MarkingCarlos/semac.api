package com.semac.java_api.dto;

/* Cada brinde é uma unidade: `entregue` diz se alguém já o ganhou. */
public record BrindeResponseDTO(
        Integer id,
        String nome,
        boolean entregue,
        Integer sorteioId,
        String sorteioNome
) {}
