package com.semac.java_api.repository;

import com.semac.java_api.model.EventoParticipante;
import com.semac.java_api.model.enums.StatusPresenca;
import com.semac.java_api.model.pk.EventoParticipantePK;
import com.semac.java_api.repository.projection.InscritosEventoView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EventoParticipanteRepository extends JpaRepository<EventoParticipante, EventoParticipantePK> {
    List<EventoParticipante> findByPk_EventoId(Integer eventoId);
    List<EventoParticipante> findByPk_ParticipanteId(Integer participanteId);
    List<EventoParticipante> findByPk_EventoIdAndStatus(Integer eventoId, StatusPresenca status);

    /* Vaga ocupada = quem está inscrito ou já teve presença registrada.
       AUSENTE não conta: o status vira AUSENTE quando o evento termina
       sem check-in, e nesse ponto a vaga não existe mais. */
    long countByPk_EventoIdAndStatusIn(Integer eventoId, Collection<StatusPresenca> status);

    /* Ocupação de todos os eventos de uma vez, para a listagem não fazer
       uma contagem por evento. */
    @Query("""
            SELECT ep.pk.eventoId AS eventoId, COUNT(ep) AS total
            FROM EventoParticipante ep
            WHERE ep.status IN :status
            GROUP BY ep.pk.eventoId
            """)
    List<InscritosEventoView> contarInscritosPorEvento(@Param("status") Collection<StatusPresenca> status);

    /* Eventos em que o participante ocupa vaga, com o evento e o tipo já
       carregados (a área do participante mostra nome, local e horário). */
    @Query("""
            SELECT ep FROM EventoParticipante ep
            JOIN FETCH ep.evento e
            JOIN FETCH e.tipoEvento
            WHERE ep.pk.participanteId = :participanteId
            """)
    List<EventoParticipante> buscarComEventoPorParticipante(@Param("participanteId") Integer participanteId);

    void deleteByPk_ParticipanteId(Integer participanteId);

    void deleteByPk_EventoId(Integer eventoId);
}
