package com.semac.java_api.dto;

/* O que a tela de teste do /admin -> Termo precisa para montar a grade:
   só o tamanho da palavra. O texto dela não vem, mesma regra do
   TermoPalavraAdminDTO. */
public record TermoTesteDTO(
        int tamanho
) {}
