package com.semac.java_api.repository;

import com.semac.java_api.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Integer> {
    List<Compra> findAllByOrderByDataCompraDesc();
}
