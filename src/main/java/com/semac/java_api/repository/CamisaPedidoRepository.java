package com.semac.java_api.repository;

import com.semac.java_api.model.CamisaPedido;
import com.semac.java_api.repository.projection.EstoqueView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CamisaPedidoRepository extends JpaRepository<CamisaPedido, Integer> {

    List<CamisaPedido> findByPessoaId(Integer pessoaId);

    @Query("SELECT c.tamanho AS tamanho, c.modelo AS modelo, COUNT(c) AS total " +
           "FROM CamisaPedido c GROUP BY c.tamanho, c.modelo")
    List<EstoqueView> consultarEstoque();
}