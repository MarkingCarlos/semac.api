package com.semac.java_api.repository;

import com.semac.java_api.model.RecuperacaoSenha;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, Integer> {

    /* Com lock pessimista: duas tentativas de código disparadas ao mesmo
       tempo não podem ler o mesmo contador e passar do limite de erros. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RecuperacaoSenha> findByPessoaId(Integer pessoaId);

    Optional<RecuperacaoSenha> findByTokenTrocaHash(String tokenTrocaHash);
}
