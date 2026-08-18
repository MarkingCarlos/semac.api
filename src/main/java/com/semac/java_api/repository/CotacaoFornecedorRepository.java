package com.semac.java_api.repository;

import com.semac.java_api.model.CotacaoFornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CotacaoFornecedorRepository extends JpaRepository<CotacaoFornecedor, Integer> {
    Optional<CotacaoFornecedor> findByCotacaoIdAndFornecedorId(Integer cotacaoId, Integer fornecedorId);
}
