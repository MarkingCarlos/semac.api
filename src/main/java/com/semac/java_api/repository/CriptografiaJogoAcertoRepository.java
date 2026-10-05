package com.semac.java_api.repository;

import com.semac.java_api.model.CriptografiaJogoAcerto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CriptografiaJogoAcertoRepository extends JpaRepository<CriptografiaJogoAcerto, Integer> {

    /* Os acertos da pessoa pelo ID */
    List<CriptografiaJogoAcerto> findByPessoaId(Integer idPessoa);

    /* O acerto do jogo de uma pessoa conta com a pessoa e a palavra. Existe no máximo um
       (unique pessoa+palavra) — é o que segura o crédito único dos xp. */
    Optional<CriptografiaJogoAcerto> findByPessoaIdAndPalavraId(Integer pessoaId, Integer palavraId);
}
