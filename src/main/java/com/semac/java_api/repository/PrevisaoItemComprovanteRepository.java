package com.semac.java_api.repository;

import com.semac.java_api.model.PrevisaoItemComprovante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PrevisaoItemComprovanteRepository extends JpaRepository<PrevisaoItemComprovante, Integer> {
    List<PrevisaoItemComprovante> findAllByPrevisaoItem_IdOrderByEnviadoEmAscIdAsc(Integer previsaoItemId);

    Optional<PrevisaoItemComprovante> findByIdAndPrevisaoItem_Id(Integer id, Integer previsaoItemId);

    long countByPrevisaoItem_Id(Integer previsaoItemId);

    /* Contagem de todos os itens numa consulta só, para a listagem da
       previsão não fazer uma contagem por linha. Cada linha: [itemId, total]. */
    @Query("select c.previsaoItem.id, count(c) from PrevisaoItemComprovante c group by c.previsaoItem.id")
    List<Object[]> contarPorItem();
}
