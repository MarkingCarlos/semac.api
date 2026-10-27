package com.semac.java_api.dto;

/* Resposta do envio de uma nota. `conquistaConcedida` = esta nota
   destravou a conquista AVALIACAO_COMPLETA agora, e o modal do
   participante já pode comemorar. */
public record AvaliacaoEventoEnviadaDTO(Integer eventoId, Integer nota, boolean conquistaConcedida) {}
