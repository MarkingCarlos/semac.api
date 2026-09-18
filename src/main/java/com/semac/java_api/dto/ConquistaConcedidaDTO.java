package com.semac.java_api.dto;

/* Retorno da concessão manual, com o que a tela de sucesso do /checkin
   precisa mostrar a quem está operando a fila. */
public record ConquistaConcedidaDTO(
        String participanteNome,
        String conquistaNome,
        Integer pontosCreditados,
        String nivelAtual,
        Integer xpTotal
) {}
