package com.semac.java_api.event;

import java.math.BigDecimal;

/* Disparado quando a comissão confirma a inscrição de alguém
   (PessoaService.atribuirRole com role = PARTICIPANTE).

   Carrega valores já extraídos, não a entidade Pessoa: o listener roda
   depois do commit, fora da transação e em outra thread, onde qualquer
   acesso lazy estouraria LazyInitializationException. */
public record InscricaoConfirmadaEvent(
        String nomeParticipante,
        String emailParticipante,
        String nomeIngresso,
        BigDecimal valorIngresso,
        Integer diasInscricao
) {
}
