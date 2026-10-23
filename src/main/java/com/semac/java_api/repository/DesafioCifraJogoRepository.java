package com.semac.java_api.repository;

import com.semac.java_api.model.DesafioCifraJogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DesafioCifraJogoRepository extends JpaRepository<DesafioCifraJogo, Integer> {

    Optional<DesafioCifraJogo> findByPessoaIdAndDesafioId(Integer pessoaId, Integer desafioId);

    /* Os jogos da pessoa num conjunto de desafios, numa consulta só — o
       histórico da aba "Desafios" monta um card por dia. */
    List<DesafioCifraJogo> findByPessoaIdAndDesafioIdIn(Integer pessoaId, Collection<Integer> desafioIds);

    long countByDesafioId(Integer desafioId);

    long countByDesafioIdAndVenceuTrue(Integer desafioId);
}
