package com.semac.java_api.dto;

/* Pessoa elegível para um sorteio: presente no evento e ainda sem prêmio
   ganho. Versão enxuta de Pessoa (sem e-mail/uuid) — essa lista alimenta
   o rolo de nomes na tela projetada de /sorteio. */
public record ParticipanteElegivelDTO(Integer id, String nome) {}
