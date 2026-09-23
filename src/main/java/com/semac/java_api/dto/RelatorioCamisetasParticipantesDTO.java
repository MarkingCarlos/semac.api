package com.semac.java_api.dto;

import java.util.List;

/* Relatório de camisetas de participantes do /admin: total de camisetas do
   modelo de participante (kit de participante/pendente + todas as avulsas)
   e o detalhamento por modelo/tamanho para o pedido ao fornecedor. */
public record RelatorioCamisetasParticipantesDTO(
        int totalParticipantes,
        List<ItemEstoqueCamisetaDTO> porModeloTamanho
) {}
