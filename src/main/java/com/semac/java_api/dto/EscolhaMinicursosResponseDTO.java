package com.semac.java_api.dto;

/* Se o participante pode entrar/sair de minicursos na edição. Ano sem
   configuração cadastrada volta fechado (false). */
public record EscolhaMinicursosResponseDTO(
        Integer ano,
        Boolean escolhaMinicursosAberta
) {}
