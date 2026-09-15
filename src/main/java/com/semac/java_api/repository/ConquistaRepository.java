package com.semac.java_api.repository;

import com.semac.java_api.model.Conquista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConquistaRepository extends JpaRepository<Conquista, Integer> {
    List<Conquista> findByRaridade(Integer raridade);
    Optional<Conquista> findByCodigo(String codigo);
}
