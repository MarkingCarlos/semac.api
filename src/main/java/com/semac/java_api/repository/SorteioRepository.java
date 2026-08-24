package com.semac.java_api.repository;

import com.semac.java_api.model.Sorteio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SorteioRepository extends JpaRepository<Sorteio, Integer> {
    List<Sorteio> findByEvento_Id(Integer eventoId);
    List<Sorteio> findByOrganizador_Id(Integer organizadorId);

    /* Quantidade já entregue de um brinde — sem coluna acumuladora,
       calculado contando os sorteios vinculados a ele (ver V9__brinde.sql). */
    long countByBrinde_Id(Integer brindeId);
}
