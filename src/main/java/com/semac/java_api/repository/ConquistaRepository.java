package com.semac.java_api.repository;

import com.semac.java_api.model.Conquista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConquistaRepository extends JpaRepository<Conquista, Integer> {

    /* `codigo` é único desde a V35, então o Optional é seguro. */
    Optional<Conquista> findByCodigo(String codigo);

    /* Catálogo completo para o /admin, na ordem em que aparece na grade. */
    List<Conquista> findAllByOrderByOrdemAscIdAsc();

    /* Vitrine do participante: só o que a presidência já liberou. */
    List<Conquista> findByAtivaTrueOrderByOrdemAscIdAsc();
}
