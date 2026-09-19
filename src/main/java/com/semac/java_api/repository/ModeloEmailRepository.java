package com.semac.java_api.repository;

import com.semac.java_api.model.ModeloEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModeloEmailRepository extends JpaRepository<ModeloEmail, Integer> {

    Optional<ModeloEmail> findByChave(String chave);

    List<ModeloEmail> findAllByOrderByChaveAsc();
}
