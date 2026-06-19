package com.semac.java_api.dto;

/* Corpo padrão de respostas de erro da API. O front exibe `mensagem`
   diretamente como aviso ao usuário. */
public record ErroRespostaDTO(String mensagem) {}
