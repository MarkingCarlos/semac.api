package com.semac.java_api.repository;

import com.semac.java_api.model.TermoJogo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TermoJogoRepository extends JpaRepository<TermoJogo, Integer> {

    /* O jogo de uma pessoa contra a palavra de um dia. Existe no máximo um
       (unique pessoa+palavra) — é o que segura o limite de tentativas e o
       crédito único dos xp. */
    Optional<TermoJogo> findByPessoaIdAndPalavraId(Integer pessoaId, Integer palavraId);

    /* Os jogos da pessoa contra um conjunto de palavras, numa consulta só —
       o histórico da aba "Desafios" monta um card por dia do evento e não
       deve custar uma ida ao banco por dia. As tentativas vêm junto porque
       é delas que sai o "em andamento" do card; sem o grafo, contá-las
       traria de volta o N+1 que o `In` acabou de evitar. */
    @EntityGraph(attributePaths = "tentativas")
    List<TermoJogo> findByPessoaIdAndPalavraIdIn(Integer pessoaId, Collection<Integer> palavraIds);
}
