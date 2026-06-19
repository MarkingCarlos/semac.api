package com.semac.java_api.repository;

import com.semac.java_api.model.Cota;
import com.semac.java_api.model.enums.NivelPatrocinio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CotaRepository extends JpaRepository<Cota, Integer> {
    List<Cota> findAllByOrderByValorAsc();
    boolean existsByNivel(NivelPatrocinio nivel);
    boolean existsByNivelAndIdNot(NivelPatrocinio nivel, Integer id);
}
