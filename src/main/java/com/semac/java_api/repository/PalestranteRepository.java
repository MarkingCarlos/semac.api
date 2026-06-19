package com.semac.java_api.repository;

import com.semac.java_api.model.Palestrante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PalestranteRepository extends JpaRepository<Palestrante, Integer> {
    List<Palestrante> findByNomeContainingIgnoreCase(String nome);
}
