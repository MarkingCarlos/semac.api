package com.semac.java_api.repository;

import com.semac.java_api.model.VitoriaTarefaMinicurso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VitoriaTarefaMinicursoRepository extends JpaRepository<VitoriaTarefaMinicurso, Integer> {

    /* Vagas já ocupadas no pódio do minicurso (máximo 3). */
    long countByEventoId(Integer eventoId);

    boolean existsByEventoIdAndPessoaId(Integer eventoId, Integer pessoaId);
}
