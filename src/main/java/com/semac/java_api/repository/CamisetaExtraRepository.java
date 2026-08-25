package com.semac.java_api.repository;

import com.semac.java_api.model.CamisetaExtra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CamisetaExtraRepository extends JpaRepository<CamisetaExtra, Integer> {
    Optional<CamisetaExtra> findByAno(Integer ano);
}
