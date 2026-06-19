package com.semac.java_api.repository;

import com.semac.java_api.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Integer> {
    List<Evento> findByTipoEvento_Id(Integer tipoEventoId);
    List<Evento> findByDataHoraInicioBetween(LocalDateTime inicio, LocalDateTime fim);
}
