package com.semac.java_api.repository;

import com.semac.java_api.model.ConfiguracaoInscricao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracaoInscricaoRepository extends JpaRepository<ConfiguracaoInscricao, Integer> {
    Optional<ConfiguracaoInscricao> findByAno(Integer ano);
}
