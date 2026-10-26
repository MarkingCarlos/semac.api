package com.semac.java_api.repository;

import com.semac.java_api.model.BrindeComissao;
import com.semac.java_api.model.Pessoa;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BrindeComissaoRepository extends JpaRepository<BrindeComissao, Integer> {

    /* Leitura no /checkin: já traz o dono e quem entregou, para a resposta
       e a mensagem de "já retirado por". */
    @EntityGraph(attributePaths = { "pessoa", "entreguePor" })
    Optional<BrindeComissao> findByCodigo(String codigo);

    /* Baixa atômica da leitura única: só atualiza se ainda não foi
       retirado. 0 = alguém já retirou (inclusive por outra câmera no mesmo
       instante). */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BrindeComissao b SET b.retiradoEm = :agora, b.entreguePor = :entreguePor " +
            "WHERE b.pessoaId = :pessoaId AND b.retiradoEm IS NULL")
    int marcarRetirado(@Param("pessoaId") Integer pessoaId,
                       @Param("agora") LocalDateTime agora,
                       @Param("entreguePor") Pessoa entreguePor);
}
