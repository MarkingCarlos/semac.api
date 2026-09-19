package com.semac.java_api.repository;

import com.semac.java_api.model.TermoJogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TermoJogoRepository extends JpaRepository<TermoJogo, Integer> {

    /* O jogo de uma pessoa contra a palavra de um dia. Existe no máximo um
       (unique pessoa+palavra) — é o que segura o limite de tentativas e o
       crédito único dos xp. */
    Optional<TermoJogo> findByPessoaIdAndPalavraId(Integer pessoaId, Integer palavraId);
}
