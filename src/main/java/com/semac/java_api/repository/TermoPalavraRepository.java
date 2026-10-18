package com.semac.java_api.repository;

import com.semac.java_api.model.TermoPalavra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TermoPalavraRepository extends JpaRepository<TermoPalavra, Integer> {

    /* As palavras de um dia, na ordem em que foram cadastradas — é o
       conjunto de onde o sorteio tira a de cada participante. */
    List<TermoPalavra> findByDiaIdOrderByIdAsc(Integer diaId);

    /* As palavras de vários dias numa consulta só, para o /admin montar os
       quatro cards sem uma ida ao banco por dia. */
    List<TermoPalavra> findByDiaIdInOrderByIdAsc(Collection<Integer> diaIds);

    /* Quais destas palavras já estão no dia — o cadastro recusa repetida,
       senão dois participantes jogariam a mesma. */
    List<TermoPalavra> findByDiaIdAndPalavraIn(Integer diaId, Collection<String> palavras);
}
