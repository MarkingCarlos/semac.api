package com.semac.java_api.dto;

/* Status de presença de um participante em um evento.
   `status` é o nome do enum StatusPresenca (AUSENTE | PRESENTE | INSCRITO),
   consumido pela tabela de participantes do /admin. */
public record PresencaParticipanteDTO(String status) {}
