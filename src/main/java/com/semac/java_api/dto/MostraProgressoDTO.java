package com.semac.java_api.dto;

import java.util.List;

/* O progresso do participante na Mostra Técnica — o card da aba Desafios.
   Lista vazia = nenhum trabalho cadastrado na edição, e o card não aparece. */
public record MostraProgressoDTO(
        int visitados,
        int total,
        List<TrabalhoMostraParticipanteDTO> trabalhos
) {}
