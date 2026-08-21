package com.semac.java_api.repository.projection;

/* Quantos participantes ocupam vaga em um evento (ver
   EventoParticipanteRepository.contarInscritosPorEvento). Usada para
   calcular as vagas restantes dos minicursos em uma query só, sem N+1. */
public interface InscritosEventoView {
    Integer getEventoId();
    Long getTotal();
}
