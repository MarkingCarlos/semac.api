package com.semac.java_api.repository.projection;

/* Quantos check-ins cada membro da comissão registrou (ver
   EventoParticipanteRepository.contarLeiturasPorOperador). Id e nome
   nulos agrupam os check-ins anteriores à V47. */
public interface LeiturasOperadorView {
    Integer getOperadorId();
    String getOperadorNome();
    Long getTotal();
}
