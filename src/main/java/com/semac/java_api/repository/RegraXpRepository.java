package com.semac.java_api.repository;

import com.semac.java_api.model.RegraXp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegraXpRepository extends JpaRepository<RegraXp, Integer> {
    Optional<RegraXp> findByChave(String chave);
    List<RegraXp> findAllByOrderByOrdemAsc();
}
