package com.semac.java_api.repository;

import com.semac.java_api.model.EventoPalestrante;
import com.semac.java_api.model.pk.EventoPalestrantePK;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoPalestranteRepository extends JpaRepository<EventoPalestrante, EventoPalestrantePK> {
    List<EventoPalestrante> findByPk_EventoId(Integer eventoId);
    List<EventoPalestrante> findByPk_PalestranteId(Integer palestranteId);
}
