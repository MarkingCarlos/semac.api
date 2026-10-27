package com.semac.java_api.repository;

import com.semac.java_api.model.TermoJogo;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.repository.projection.ContagemJogosPalavraTermoView;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TermoJogoRepository extends JpaRepository<TermoJogo, Integer> {

    /* O jogo de uma pessoa num dia. Existe no máximo um (unique
       pessoa+dia) — é o que segura o limite de tentativas, o crédito único
       dos xp e a palavra sorteada. */
    Optional<TermoJogo> findByPessoaIdAndDiaId(Integer pessoaId, Integer diaId);

    /* Os jogos da pessoa num conjunto de dias, numa consulta só — o
       histórico da aba "Desafios" monta um card por dia do evento e não
       deve custar uma ida ao banco por dia. As tentativas vêm junto porque
       é delas que sai o "em andamento" do card; sem o grafo, contá-las
       traria de volta o N+1 que o `In` acabou de evitar. */
    @EntityGraph(attributePaths = "tentativas")
    List<TermoJogo> findByPessoaIdAndDiaIdIn(Integer pessoaId, Collection<Integer> diaIds);

    @Query("""
            SELECT j.palavra.id AS palavraId, COUNT(j) AS total
              FROM TermoJogo j
             WHERE j.palavra.id IN :palavraIds
             GROUP BY j.palavra.id
            """)
    List<ContagemJogosPalavraTermoView> contarJogosPorPalavra(@Param("palavraIds") Collection<Integer> palavraIds);

    /* Conquista LIGEIRINHO: as vitórias de uma pessoa e o primeiro
       participante a vencer o Termo de uma data (desempate pelo id, a
       ordem de gravação). */
    List<TermoJogo> findByPessoaIdAndVenceuTrue(Integer pessoaId);

    Optional<TermoJogo> findFirstByDiaDataAndVenceuTrueAndPessoaRoleOrderByEncerradoEmAscIdAsc(LocalDate data, Role role);

    /* Palavra já sorteada para alguém não pode ser removida: apagá-la
       levaria junto (ON DELETE CASCADE) o jogo e as tentativas da pessoa. */
    boolean existsByPalavraId(Integer palavraId);
}
