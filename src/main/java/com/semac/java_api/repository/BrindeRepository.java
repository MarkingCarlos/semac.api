package com.semac.java_api.repository;

import com.semac.java_api.model.Brinde;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrindeRepository extends JpaRepository<Brinde, Integer> {
    List<Brinde> findBySorteio_Id(Integer sorteioId);
    boolean existsBySorteio_Id(Integer sorteioId);
}
