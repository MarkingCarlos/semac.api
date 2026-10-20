package com.semac.java_api.repository;

import com.semac.java_api.model.VisitaTrabalhoMostra;
import com.semac.java_api.repository.projection.ContagemVisitasTrabalhoMostraView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface VisitaTrabalhoMostraRepository extends JpaRepository<VisitaTrabalhoMostra, Integer> {

    boolean existsByPessoaIdAndTrabalhoId(Integer pessoaId, Integer trabalhoId);

    /* Ids dos trabalhos de uma edição que a pessoa já visitou — o progresso
       da aba Desafios e da página do stand. */
    @Query("""
            SELECT v.trabalho.id FROM VisitaTrabalhoMostra v
             WHERE v.pessoa.id = :pessoaId AND v.trabalho.ano = :ano
            """)
    List<Integer> idsTrabalhosVisitados(@Param("pessoaId") Integer pessoaId, @Param("ano") Integer ano);

    /* O numerador da conquista MOSTRA_COMPLETA. */
    long countByPessoaIdAndTrabalhoAno(Integer pessoaId, Integer ano);

    /* Quantas visitas cada trabalho já recebeu, para o /admin. Trabalho sem
       visita nenhuma não aparece. */
    @Query("""
            SELECT v.trabalho.id AS trabalhoId, COUNT(v) AS total
              FROM VisitaTrabalhoMostra v
             WHERE v.trabalho.id IN :trabalhoIds
             GROUP BY v.trabalho.id
            """)
    List<ContagemVisitasTrabalhoMostraView> contarVisitasPorTrabalho(@Param("trabalhoIds") Collection<Integer> trabalhoIds);
}
