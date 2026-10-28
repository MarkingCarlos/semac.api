package com.semac.java_api.repository;

import com.semac.java_api.model.RespostaQuizLaboratorio;
import com.semac.java_api.repository.projection.ContagemRespostasLaboratorioView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RespostaQuizLaboratorioRepository extends JpaRepository<RespostaQuizLaboratorio, Integer> {

    Optional<RespostaQuizLaboratorio> findByPessoaIdAndLaboratorioId(Integer pessoaId, Integer laboratorioId);

    /* Grava a tentativa única. O UNIQUE (pessoa_id, laboratorio_id) faz a
       trava — 0 = já respondeu (inclusive por um envio duplo no mesmo
       instante), e aí ninguém credita XP de novo. */
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO resposta_quiz_laboratorio " +
            "(pessoa_id, laboratorio_id, acertos, total, xp_creditado, respondido_em) " +
            "VALUES (:pessoaId, :laboratorioId, :acertos, :total, :xpCreditado, :agora) " +
            "ON CONFLICT (pessoa_id, laboratorio_id) DO NOTHING", nativeQuery = true)
    int registrar(@Param("pessoaId") Integer pessoaId,
                  @Param("laboratorioId") Integer laboratorioId,
                  @Param("acertos") int acertos,
                  @Param("total") int total,
                  @Param("xpCreditado") int xpCreditado,
                  @Param("agora") LocalDateTime agora);

    /* Contadores do /admin: quantos responderam e quantos gabaritaram cada
       laboratório. Laboratório sem resposta não aparece. */
    @Query("""
            SELECT r.laboratorioId AS laboratorioId,
                   COUNT(r) AS respostas,
                   SUM(CASE WHEN r.acertos = r.total AND r.total > 0 THEN 1 ELSE 0 END) AS gabaritos
              FROM RespostaQuizLaboratorio r
             WHERE r.laboratorioId IN :laboratorioIds
             GROUP BY r.laboratorioId
            """)
    List<ContagemRespostasLaboratorioView> contarPorLaboratorio(@Param("laboratorioIds") Collection<Integer> laboratorioIds);
}
