package com.semac.java_api.repository;

import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PessoaRepository extends JpaRepository<Pessoa, Integer> {
    Optional<Pessoa> findByEmail(String email);
    Optional<Pessoa> findByCpf(String cpf);
    Optional<Pessoa> findByUuid(String uuid);
    List<Pessoa> findAllByRole(Role role);
    List<Pessoa> findAllByRoleNot(Role role);

    /* Participantes do /admin: confirmados (role = PARTICIPANTE) e os
       recém-inscritos aguardando confirmação (role = NULL). Exclui
       organizadores (demais roles). Ordenado por nome. */
    List<Pessoa> findByRoleIsNullOrRoleOrderByNomeAsc(Role role);
}
