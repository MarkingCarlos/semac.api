package com.semac.java_api.repository;

import com.semac.java_api.model.TrabalhoMostra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrabalhoMostraRepository extends JpaRepository<TrabalhoMostra, Integer> {

    /* Os trabalhos de uma edição, na ordem em que foram cadastrados. */
    List<TrabalhoMostra> findByAnoOrderByIdAsc(Integer ano);

    /* O denominador da conquista MOSTRA_COMPLETA. */
    long countByAno(Integer ano);

    Optional<TrabalhoMostra> findByCodigoQr(String codigoQr);

    boolean existsByCodigoQr(String codigoQr);
}
