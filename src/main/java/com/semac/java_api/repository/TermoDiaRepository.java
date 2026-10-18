package com.semac.java_api.repository;

import com.semac.java_api.model.TermoDia;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TermoDiaRepository extends JpaRepository<TermoDia, Integer> {

    /* O dia que está valendo hoje — é a data que decide. */
    Optional<TermoDia> findByData(LocalDate data);

    Optional<TermoDia> findByAnoAndNumero(Integer ano, Integer numero);

    List<TermoDia> findByAnoOrderByNumeroAsc(Integer ano);

    /* Os dias que já aconteceram, para o histórico da aba "Desafios". O
       corte por data é o que mantém os dias ainda por vir fora da
       resposta: saber que existe Termo amanhã não é informação do
       participante. */
    List<TermoDia> findByAnoAndDataLessThanEqualOrderByNumeroAsc(Integer ano, LocalDate ate);

    /* Usada para saber se o dia de hoje é o último cadastrado — o modal de
       derrota muda de texto no último dia ("não foi dessa vez" em vez de
       "volte amanhã"). */
    Optional<TermoDia> findTopByAnoOrderByNumeroDesc(Integer ano);

    /* `data` é única: a checagem evita estourar a constraint com um 500
       quando a diretoria repete a data em dois dias. */
    Optional<TermoDia> findByDataAndIdNot(LocalDate data, Integer id);

    /* Trava a linha do dia (SELECT ... FOR UPDATE) enquanto o sorteio
       conta quantos jogos cada palavra tem e grava o novo — sem isso, duas
       pessoas abrindo o /termo ao mesmo tempo veem a mesma palavra livre e
       saem com ela as duas. Mesmo recurso de EventoRepository.buscarParaInscricao. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM TermoDia d WHERE d.id = :id")
    Optional<TermoDia> buscarParaSorteio(@Param("id") Integer id);
}
