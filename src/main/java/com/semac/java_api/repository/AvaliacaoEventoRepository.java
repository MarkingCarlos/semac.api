package com.semac.java_api.repository;

import com.semac.java_api.model.AvaliacaoEvento;
import com.semac.java_api.repository.projection.ResumoAvaliacaoEventoView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AvaliacaoEventoRepository extends JpaRepository<AvaliacaoEvento, Integer> {

    Optional<AvaliacaoEvento> findByEventoIdAndPessoaId(Integer eventoId, Integer pessoaId);

    /* Notas do próprio participante (área /participantes) e base da
       conquista AVALIACAO_COMPLETA. */
    List<AvaliacaoEvento> findByPessoaId(Integer pessoaId);

    /* Resumo do /admin: total e média de cada evento avaliado. */
    @Query("""
            SELECT a.evento.id AS eventoId, COUNT(a) AS total, AVG(a.nota) AS media
            FROM AvaliacaoEvento a
            GROUP BY a.evento.id
            """)
    List<ResumoAvaliacaoEventoView> resumirPorEvento();
}
