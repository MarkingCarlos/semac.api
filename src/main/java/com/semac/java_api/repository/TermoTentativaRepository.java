package com.semac.java_api.repository;

import com.semac.java_api.model.TermoTentativa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TermoTentativaRepository extends JpaRepository<TermoTentativa, Integer> {
    List<TermoTentativa> findByJogoIdOrderByOrdemAsc(Integer jogoId);
}
