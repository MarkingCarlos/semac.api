package com.semac.java_api.repository;

import com.semac.java_api.model.TipoInscricao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoInscricaoRepository extends JpaRepository<TipoInscricao, Integer> {
    List<TipoInscricao> findByAnoOrderByNomeAsc(Integer ano);
    boolean existsByNomeAndAno(String nome, Integer ano);
    boolean existsByNomeAndAnoAndIdNot(String nome, Integer ano, Integer id);
}
