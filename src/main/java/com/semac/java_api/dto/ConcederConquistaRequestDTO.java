package com.semac.java_api.dto;

/* Identifica quem recebe uma conquista manual. `uuid` é o do crachá, lido
   pelo QR code em /checkin — é o caminho normal. `participanteId` é a
   saída manual, para quando a leitura falha e a pessoa é encontrada pela
   busca por nome, mesmo par de caminhos do check-in de presença. Um dos
   dois vem preenchido. */
public record ConcederConquistaRequestDTO(String uuid, Integer participanteId) {}
