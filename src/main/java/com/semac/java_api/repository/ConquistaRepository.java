package com.semac.java_api.repository;

import com.semac.java_api.model.Conquista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConquistaRepository extends JpaRepository<Conquista, Integer> {
    List<Conquista> findByRaridade(Integer raridade);
}
