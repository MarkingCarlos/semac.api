package com.semac.java_api.repository;

import com.semac.java_api.model.EventoParticipante;
import com.semac.java_api.model.enums.StatusPresenca;
import com.semac.java_api.model.pk.EventoParticipantePK;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoParticipanteRepository extends JpaRepository<EventoParticipante, EventoParticipantePK> {
    List<EventoParticipante> findByPk_EventoId(Integer eventoId);
    List<EventoParticipante> findByPk_ParticipanteId(Integer participanteId);
    List<EventoParticipante> findByPk_EventoIdAndStatus(Integer eventoId, StatusPresenca status);
}
