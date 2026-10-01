package com.semac.java_api.repository;

import com.semac.java_api.model.Evento;
import com.semac.java_api.model.enums.CodigoTipoEvento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventoRepository extends JpaRepository<Evento, Integer> {
    List<Evento> findByTipoEvento_Id(Integer tipoEventoId);
    List<Evento> findByDataHoraInicioBetween(LocalDateTime inicio, LocalDateTime fim);

    /* Eventos abertos (palestra, mesa redonda, debate): todo participante
       confirmado é pré-inscrito neles, sem checagem de capacidade. */
    List<Evento> findByTipoEvento_ExigeInscricaoFalse();

    /* Eventos dos tipos dados, já com o tipo carregado — base das
       conquistas que contam presença por tipo (ver ConquistaService). */
    @Query("SELECT e FROM Evento e JOIN FETCH e.tipoEvento t WHERE t.codigo IN :codigos")
    List<Evento> buscarComTipoPorCodigos(@Param("codigos") Collection<CodigoTipoEvento> codigos);

    /* Minicursos (tipos que exigem inscrição e têm lotação) — card de
       vagas da dashboard do /admin. */
    List<Evento> findByTipoEvento_ExigeInscricaoTrueOrderByDataHoraInicioAsc();

    /* Trava a linha do evento (SELECT ... FOR UPDATE) enquanto a inscrição
       em minicurso conta as vagas e grava — sem isso, duas pessoas podem
       passar pela contagem ao mesmo tempo e estourar a lotação. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Evento e WHERE e.id = :id")
    Optional<Evento> buscarParaInscricao(@Param("id") Integer id);
}
