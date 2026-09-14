package com.semac.java_api.repository;

import com.semac.java_api.model.PrevisaoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrevisaoItemRepository extends JpaRepository<PrevisaoItem, Integer> {
    List<PrevisaoItem> findAllByOrderByCategoria_OrdemAscIdAsc();

    boolean existsByCategoria_Id(Integer categoriaId);

    boolean existsByFornecedor_Id(Integer fornecedorId);
}
