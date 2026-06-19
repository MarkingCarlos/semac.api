package com.semac.java_api.repository;

import com.semac.java_api.model.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoEventoRepository extends JpaRepository<TipoEvento, Integer> {
    Optional<TipoEvento> findByNome(String nome);
    boolean existsByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Integer id);
}
