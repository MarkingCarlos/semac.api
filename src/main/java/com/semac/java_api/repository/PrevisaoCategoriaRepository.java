package com.semac.java_api.repository;

import com.semac.java_api.model.PrevisaoCategoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrevisaoCategoriaRepository extends JpaRepository<PrevisaoCategoria, Integer> {
    List<PrevisaoCategoria> findAllByOrderByOrdemAscNomeAsc();

    Optional<PrevisaoCategoria> findByNomeIgnoreCase(String nome);
}
