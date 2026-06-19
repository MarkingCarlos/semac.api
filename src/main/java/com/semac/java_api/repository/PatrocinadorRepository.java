package com.semac.java_api.repository;

import com.semac.java_api.model.Patrocinador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatrocinadorRepository extends JpaRepository<Patrocinador, Integer> {
    List<Patrocinador> findAllByOrderByNomeAsc();
}
