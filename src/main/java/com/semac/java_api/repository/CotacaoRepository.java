package com.semac.java_api.repository;

import com.semac.java_api.model.Cotacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CotacaoRepository extends JpaRepository<Cotacao, Integer> {
    List<Cotacao> findAllByOrderByDescricaoAsc();
}
