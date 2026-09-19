package com.semac.java_api.dto;

/* Uma variável disponível para a mensagem. `exemplo` é o valor usado na
   prévia do /admin, antes de a mensagem ir para alguém de verdade. */
public record VariavelEmailDTO(String nome, String descricao, String exemplo) {}
