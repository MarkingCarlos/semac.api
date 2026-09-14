package com.semac.java_api.repository;

import com.semac.java_api.model.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Integer> {
    Optional<Orcamento> findByAno(Integer ano);

    /* O módulo trabalha sempre com a edição mais recente. */
    Optional<Orcamento> findFirstByOrderByAnoDesc();
}
