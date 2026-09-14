package com.semac.java_api.repository;

import com.semac.java_api.model.Caixa;
import com.semac.java_api.model.enums.ContaFinanceira;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaixaRepository extends JpaRepository<Caixa, Integer> {
    /* Uma linha por conta — garantido por UNIQUE desde a V28. */
    Optional<Caixa> findByConta(ContaFinanceira conta);

    List<Caixa> findAllByOrderByContaAsc();

    /* Usado antes de excluir uma pessoa: se ela foi quem fez o último
       ajuste de algum caixa, a exclusão é bloqueada para preservar a
       auditoria. */
    boolean existsByAtualizadoPor_Id(Integer pessoaId);
}
