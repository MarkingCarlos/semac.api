package com.semac.java_api.repository;

import com.semac.java_api.model.BrindeComissao;
import com.semac.java_api.model.Pessoa;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface BrindeComissaoRepository extends JpaRepository<BrindeComissao, Integer> {

    /* Leitura no /checkin: já traz o dono e quem entregou, para a resposta
       e a mensagem de "já retirado por". */
    @EntityGraph(attributePaths = { "pessoa", "entreguePor" })
    Optional<BrindeComissao> findByCodigo(String codigo);

    @EntityGraph(attributePaths = { "pessoa", "entreguePor" })
    Optional<BrindeComissao> findByPessoaIdAndDia(Integer pessoaId, LocalDate dia);

    /* Baixa atômica da leitura única do QR da comissão: só atualiza se
       ainda não foi retirado. 0 = alguém já retirou (inclusive por outra
       câmera no mesmo instante). */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BrindeComissao b SET b.retiradoEm = :agora, b.entreguePor = :entreguePor " +
            "WHERE b.id = :id AND b.retiradoEm IS NULL")
    int marcarRetirado(@Param("id") Integer id,
                       @Param("agora") LocalDateTime agora,
                       @Param("entreguePor") Pessoa entreguePor);

    /* Retirada pelo crachá (participante): grava a linha do dia já
       retirada. O UNIQUE (pessoa_id, dia) faz a leitura única — 0 = já
       retirou hoje (inclusive por outra câmera no mesmo instante). */
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO brinde_comissao (pessoa_id, dia, retirado_em, entregue_por_id) " +
            "VALUES (:pessoaId, :dia, :agora, :entreguePorId) " +
            "ON CONFLICT (pessoa_id, dia) DO NOTHING", nativeQuery = true)
    int registrarRetiradaPorCracha(@Param("pessoaId") Integer pessoaId,
                                   @Param("dia") LocalDate dia,
                                   @Param("agora") LocalDateTime agora,
                                   @Param("entreguePorId") Integer entreguePorId);
}
