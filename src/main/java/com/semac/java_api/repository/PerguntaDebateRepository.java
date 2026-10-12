package com.semac.java_api.repository;

import com.semac.java_api.model.PerguntaDebate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerguntaDebateRepository extends JpaRepository<PerguntaDebate, Integer> {

    /* Lista do /admin: já traz a pessoa para mostrar o nome sem N+1. */
    @EntityGraph(attributePaths = "pessoa")
    List<PerguntaDebate> findByEventoIdOrderByEnviadaEmAsc(Integer eventoId);

    long countByEventoIdAndPessoaId(Integer eventoId, Integer pessoaId);
}
