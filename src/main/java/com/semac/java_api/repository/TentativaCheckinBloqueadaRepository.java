package com.semac.java_api.repository;

import com.semac.java_api.model.TentativaCheckinBloqueada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TentativaCheckinBloqueadaRepository extends JpaRepository<TentativaCheckinBloqueada, Integer> {

    /* As duas leituras naturais do log: o que aconteceu num evento e o
       que um operador andou tentando. Mais recentes primeiro. */
    List<TentativaCheckinBloqueada> findByEventoIdOrderByTentadoEmDesc(Integer eventoId);

    List<TentativaCheckinBloqueada> findByOperadorIdOrderByTentadoEmDesc(Integer operadorId);
}
