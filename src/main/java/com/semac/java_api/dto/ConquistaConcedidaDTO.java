package com.semac.java_api.dto;

/* Retorno da concessão manual, com o que a tela de sucesso do /checkin
   precisa mostrar a quem está operando a fila. `detalhe` é um texto extra
   opcional (ex.: "Desenvolvimento Web · 2º de 3" na ALQUIMIA_COMPLETA);
   `jaTinha` indica que a pessoa já tinha a conquista e nada foi creditado. */
public record ConquistaConcedidaDTO(
        String participanteNome,
        String conquistaNome,
        Integer pontosCreditados,
        String nivelAtual,
        Integer xpTotal,
        String detalhe,
        boolean jaTinha
) {}
