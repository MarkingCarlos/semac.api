package com.semac.java_api.repository;

import com.semac.java_api.model.CaixaFundunesp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaixaFundunespRepository extends JpaRepository<CaixaFundunesp, Integer> {
    /* A tabela guarda um registro único — sempre o primeiro. */
    Optional<CaixaFundunesp> findFirstByOrderByIdAsc();
}
