package com.semac.java_api.repository;

import com.semac.java_api.model.MetaDoacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MetaDoacaoRepository extends JpaRepository<MetaDoacao, Integer> {
    Optional<MetaDoacao> findByAno(Integer ano);
}
