package com.semac.java_api.repository;

import com.semac.java_api.model.Comunicado;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComunicadoRepository extends JpaRepository<Comunicado, Integer> {

    List<Comunicado> findAllByOrderByCriadoEmDesc(Limit limite);
}
