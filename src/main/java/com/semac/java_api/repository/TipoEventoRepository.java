package com.semac.java_api.repository;

import com.semac.java_api.model.TipoEvento;
import com.semac.java_api.model.enums.CodigoTipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoEventoRepository extends JpaRepository<TipoEvento, Integer> {
    Optional<TipoEvento> findByCodigo(CodigoTipoEvento codigo);
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Integer id);
}
