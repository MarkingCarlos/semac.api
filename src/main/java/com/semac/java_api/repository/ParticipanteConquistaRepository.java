package com.semac.java_api.repository;

import com.semac.java_api.model.ParticipanteConquista;
import com.semac.java_api.model.pk.ParticipanteConquistaPK;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipanteConquistaRepository extends JpaRepository<ParticipanteConquista, ParticipanteConquistaPK> {
    List<ParticipanteConquista> findByPk_ParticipanteId(Integer participanteId);
    boolean existsByPk_ParticipanteIdAndPk_ConquistaId(Integer participanteId, Integer conquistaId);
}
