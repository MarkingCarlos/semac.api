package com.semac.java_api.repository;

import com.semac.java_api.model.GanhadoresSorteio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GanhadoresSorteioRepository extends JpaRepository<GanhadoresSorteio, Integer> {
    List<GanhadoresSorteio> findByParticipante_Id(Integer participanteId);
    boolean existsByParticipante_Id(Integer participanteId);
    Optional<GanhadoresSorteio> findBySorteio_Id(Integer sorteioId);
}
