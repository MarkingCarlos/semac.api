package com.semac.java_api.dto;

/* Um trabalho da Mostra Técnica na lista de progresso do participante.
   Sem o código do QR: com ele a pessoa marcaria a visita sem passar pelo
   stand. */
public record TrabalhoMostraParticipanteDTO(
        Integer id,
        String titulo,
        String apresentadores,
        String stand,
        boolean visitado
) {}
