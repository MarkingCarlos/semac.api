package com.semac.java_api.dto;

/* Quem está operando a ferramenta /checkin, lido das claims do token no
   controller (id, nome e role). Só serve para o log de tentativas fora da
   janela — o check-in em si não depende de quem o fez. */
public record OperadorCheckinDTO(
        Integer id,
        String nome,
        String role
) {}
