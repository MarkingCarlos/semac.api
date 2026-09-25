package com.semac.java_api.repository;

import com.semac.java_api.model.PessoaDiaIngresso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PessoaDiaIngressoRepository extends JpaRepository<PessoaDiaIngresso, Integer> {
    List<PessoaDiaIngresso> findByPessoaIdOrderByDiaAsc(Integer pessoaId);

    /* Checagem do check-in: o diarista escolheu este dia? */
    boolean existsByPessoaIdAndDia(Integer pessoaId, LocalDate dia);
}
