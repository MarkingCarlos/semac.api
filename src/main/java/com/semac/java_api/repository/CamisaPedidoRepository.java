package com.semac.java_api.repository;

import com.semac.java_api.model.CamisaPedido;
import com.semac.java_api.repository.projection.ContagemCamisetaGrupoView;
import com.semac.java_api.repository.projection.EstoqueView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CamisaPedidoRepository extends JpaRepository<CamisaPedido, Integer> {

    List<CamisaPedido> findByPessoaId(Integer pessoaId);
    void deleteByPessoaId(Integer pessoaId);

    /* Quantas camisetas avulsas (fora as inclusas no ingresso) a pessoa
       pediu — usado para recalcular o valor total da inscrição no cartão. */
    long countByPessoaIdAndAvulsaTrue(Integer pessoaId);

    /* Todos os pedidos, inclusive os de inscrições ainda aguardando
       confirmação no /admin (role null) — o relatório serve para fechar a
       compra, então precisa contar quem já pediu mesmo sem confirmação. */
    @Query("SELECT c.tamanho AS tamanho, c.modelo AS modelo, COUNT(c) AS total " +
           "FROM CamisaPedido c JOIN c.pessoa p " +
           "GROUP BY c.tamanho, c.modelo")
    List<EstoqueView> consultarEstoque();

    /* Quantas camisetas existem por avulsa (true/false) e role da pessoa.
       Base do relatório de camisetas — dadas/avulsas e comissão/participantes
       vêm direto do campo `avulsa`, editável no /admin. Inclui pendentes
       (role null), que o serviço trata como participantes. */
    @Query("SELECT c.avulsa AS avulsa, p.role AS role, COUNT(c) AS total " +
           "FROM CamisaPedido c JOIN c.pessoa p " +
           "GROUP BY c.avulsa, p.role")
    List<ContagemCamisetaGrupoView> contarCamisetasPorAvulsaERole();
}