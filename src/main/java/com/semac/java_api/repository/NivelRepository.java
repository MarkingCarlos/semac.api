package com.semac.java_api.repository;

import com.semac.java_api.model.Nivel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NivelRepository extends JpaRepository<Nivel, Integer> {
    Optional<Nivel> findByNome(String nome);
    boolean existsByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Integer id);
    boolean existsByXpMinimo(Integer xpMinimo);
    boolean existsByXpMinimoAndIdNot(Integer xpMinimo, Integer id);

    /* Resolve o nível correspondente a um xp acumulado: o maior xpMinimo
       que a pessoa já alcançou. */
    Optional<Nivel> findTopByXpMinimoLessThanEqualOrderByXpMinimoDesc(Integer xp);

    /* Próximo nível a alcançar: o menor xpMinimo acima do xp atual. Vazio
       quando a pessoa já está no nível mais alto cadastrado. */
    Optional<Nivel> findTopByXpMinimoGreaterThanOrderByXpMinimoAsc(Integer xp);
}
