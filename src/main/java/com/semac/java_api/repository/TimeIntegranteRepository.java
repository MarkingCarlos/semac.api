package com.semac.java_api.repository;

import com.semac.java_api.model.TimeIntegrante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TimeIntegranteRepository extends JpaRepository<TimeIntegrante, Integer> {

    long countByTime_Id(Integer timeId);

    long countByTime_IdAndReveladoEmIsNotNull(Integer timeId);

    long countByReveladoEmIsNotNull();

    /* Soma do xp dos integrantes de cada time: [time_id, soma]. Times sem
       ninguém não aparecem — quem chama trata como zero. */
    @Query("SELECT ti.time.id, COALESCE(SUM(p.xp), 0) FROM TimeIntegrante ti, Pessoa p " +
            "WHERE p.id = ti.pessoaId GROUP BY ti.time.id")
    List<Object[]> somarXpPorTime();

    /* Refazer o sorteio apaga tudo de uma vez, sem carregar as entidades. */
    @Modifying
    @Query("DELETE FROM TimeIntegrante")
    void apagarTodos();
}
