package com.semac.java_api.dto;

/* Resposta de um palpite: só o padrão de cores, nunca a palavra — esse é o
   ponto todo de a conferência ser no servidor.

   `palavra` segue a mesma regra do TermoEstadoDTO: null até o jogo
   encerrar. */
public record TermoPalpiteRespostaDTO(
        String resultado,
        boolean venceu,
        boolean encerrado,
        int restantes,
        Integer xpGanho,
        boolean ultimoDia,
        String palavra
) {}
