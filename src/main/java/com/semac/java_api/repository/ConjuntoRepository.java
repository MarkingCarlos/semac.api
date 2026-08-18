package com.semac.java_api.repository;

import com.semac.java_api.model.Conjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConjuntoRepository extends JpaRepository<Conjunto, Integer> {
    List<Conjunto> findAllByOrderByNomeAsc();
}
