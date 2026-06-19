package com.semac.java_api.repository;

import com.semac.java_api.model.Doador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoadorRepository extends JpaRepository<Doador, Integer> {
    List<Doador> findAllByOrderByDataDesc();
}
