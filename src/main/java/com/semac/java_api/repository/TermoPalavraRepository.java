package com.semac.java_api.repository;

import com.semac.java_api.model.TermoPalavra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TermoPalavraRepository extends JpaRepository<TermoPalavra, Integer> {

    /* A palavra que está valendo hoje — é a data que decide o dia ativo. */
    Optional<TermoPalavra> findByData(LocalDate data);

    Optional<TermoPalavra> findByAnoAndDia(Integer ano, Integer dia);

    List<TermoPalavra> findByAnoOrderByDiaAsc(Integer ano);

    /* Usada para saber se o dia de hoje é o último cadastrado — o modal de
       derrota muda de texto no último dia ("não foi dessa vez" em vez de
       "volte amanhã"). */
    Optional<TermoPalavra> findTopByAnoOrderByDiaDesc(Integer ano);

    /* `data` é única: a checagem evita estourar a constraint com um 500
       quando a diretoria repete a data em dois dias. */
    Optional<TermoPalavra> findByDataAndIdNot(LocalDate data, Integer id);
}
