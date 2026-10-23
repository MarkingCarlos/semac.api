package com.semac.java_api.repository;

import com.semac.java_api.model.PerguntaDebate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerguntaDebateRepository extends JpaRepository<PerguntaDebate, Integer> {

    /* Lista do /admin: já traz a pessoa (e quem cadastrou) para mostrar os
       nomes sem N+1. */
    @EntityGraph(attributePaths = { "pessoa", "cadastradaPor" })
    List<PerguntaDebate> findByEventoIdOrderByEnviadaEmAsc(Integer eventoId);

    long countByEventoIdAndPessoaId(Integer eventoId, Integer pessoaId);

    /* Conquista PERGUNTA_DEBATE: basta uma pergunta, de qualquer debate e
       de qualquer edição. */
    boolean existsByPessoaId(Integer pessoaId);
}
