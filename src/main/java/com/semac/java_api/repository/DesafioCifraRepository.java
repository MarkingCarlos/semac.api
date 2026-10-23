package com.semac.java_api.repository;

import com.semac.java_api.model.DesafioCifra;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DesafioCifraRepository extends JpaRepository<DesafioCifra, Integer> {

    /* O desafio que está valendo hoje — é a data que decide. */
    Optional<DesafioCifra> findByData(LocalDate data);

    List<DesafioCifra> findByAnoOrderByDataAsc(Integer ano);

    /* Os desafios que já aconteceram, para o histórico da aba "Desafios".
       Mesmo corte de TermoDiaRepository: os dias por vir ficam de fora. */
    List<DesafioCifra> findByAnoAndDataLessThanEqualOrderByDataAsc(Integer ano, LocalDate ate);

    /* Trava a linha do desafio enquanto o jogo da pessoa é criado ou uma
       tentativa é conferida. Sem isso, duas respostas enviadas ao mesmo
       tempo leem as mesmas tentativas restantes e passam as duas — e a
       criação concorrente do jogo estoura o unique. Mesmo recurso de
       TermoDiaRepository.buscarParaSorteio. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DesafioCifra d WHERE d.id = :id")
    Optional<DesafioCifra> buscarParaJogar(@Param("id") Integer id);
}
