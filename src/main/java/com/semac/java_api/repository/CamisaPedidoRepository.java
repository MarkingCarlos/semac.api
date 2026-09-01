package com.semac.java_api.repository;

import com.semac.java_api.model.CamisaPedido;
import com.semac.java_api.repository.projection.ContagemCamisetaPessoaView;
import com.semac.java_api.repository.projection.EstoqueView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CamisaPedidoRepository extends JpaRepository<CamisaPedido, Integer> {

    List<CamisaPedido> findByPessoaId(Integer pessoaId);
    void deleteByPessoaId(Integer pessoaId);

    @Query("SELECT c.tamanho AS tamanho, c.modelo AS modelo, COUNT(c) AS total " +
           "FROM CamisaPedido c GROUP BY c.tamanho, c.modelo")
    List<EstoqueView> consultarEstoque();

    /* Uma linha por pessoa: quantas camisetas ela já pediu e o
       camisetasGratis do ingresso vinculado (null se ela não tiver mais
       ingresso — caso da comissão). Base do relatório de camisetas. */
    @Query("SELECT COUNT(c) AS totalCamisetas, ti.camisetasGratis AS camisetasGratis " +
           "FROM CamisaPedido c JOIN c.pessoa p LEFT JOIN p.tipoInscricao ti " +
           "GROUP BY p.id, ti.camisetasGratis")
    List<ContagemCamisetaPessoaView> contarCamisetasPorPessoa();
}