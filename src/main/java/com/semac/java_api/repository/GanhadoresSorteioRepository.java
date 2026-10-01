package com.semac.java_api.repository;

import com.semac.java_api.model.GanhadoresSorteio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GanhadoresSorteioRepository extends JpaRepository<GanhadoresSorteio, Integer> {
    List<GanhadoresSorteio> findByParticipante_Id(Integer participanteId);
    boolean existsByParticipante_Id(Integer participanteId);
    List<GanhadoresSorteio> findBySorteio_IdOrderByGanhouEmAsc(Integer sorteioId);
    void deleteByParticipante_Id(Integer participanteId);

    /* Quem realizou algum sorteio não pode ser excluído (ver PessoaService.excluir). */
    boolean existsByOrganizador_Id(Integer organizadorId);

    /* Quantidade já entregue de um brinde — sem coluna acumuladora,
       calculado contando as entregas vinculadas a ele. */
    long countByBrinde_Id(Integer brindeId);
    long countBySorteio_Id(Integer sorteioId);
}
