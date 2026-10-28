package com.semac.java_api.repository;

import com.semac.java_api.model.Laboratorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LaboratorioRepository extends JpaRepository<Laboratorio, Integer> {

    /* Os laboratórios de uma edição, na ordem em que foram cadastrados. */
    List<Laboratorio> findByAnoOrderByIdAsc(Integer ano);

    Optional<Laboratorio> findByCodigoQr(String codigoQr);

    boolean existsByCodigoQr(String codigoQr);
}
