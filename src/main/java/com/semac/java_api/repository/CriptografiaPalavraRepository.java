package com.semac.java_api.repository;

import com.semac.java_api.model.CriptografiaPalavra;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CriptografiaPalavraRepository extends JpaRepository<CriptografiaPalavra, Integer> {
    
    /* Lista todas as palavras do desafio da criptografia. */
    List<CriptografiaPalavra> findAll();

    /* Lista as palavras do desafio da criptografia para uma data específica. */
    List<CriptografiaPalavra> findByData(LocalDate data);
}
