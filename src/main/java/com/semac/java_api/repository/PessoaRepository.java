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
    Optional<Pessoa> findByMpPaymentId(Long mpPaymentId);
    List<Pessoa> findAllByRole(Role role);
    List<Pessoa> findAllByRoleNot(Role role);

    /* Participantes do /admin: confirmados (role = PARTICIPANTE) e os
       recém-inscritos aguardando confirmação (role = NULL). Exclui
       organizadores (demais roles). Ordenado por nome. */
    List<Pessoa> findByRoleIsNullOrRoleOrderByNomeAsc(Role role);

    /* Mesma condicao da listagem acima, sem ordenacao: e o conjunto que o
       modulo financeiro soma no saldo da comissao (ver
       PessoaService.listarInscricoes). */
    List<Pessoa> findAllByRoleIsNullOrRole(Role role);

    /* Mesma condicao da listagem acima, so que contando: e o numero de
       inscritos que alimenta a escala POR_INSCRITO da previsao de gastos
       (ver PrevisaoService.fatoresVigentes). */
    long countByRoleIsNullOrRole(Role role);

    /* Comissao organizadora: role definido e diferente de PARTICIPANTE.
       `role <> ?` ja exclui NULL em SQL, entao pendentes ficam de fora --
       mesmo conjunto de findAllByRoleNot, usado por listarComissao. */
    long countByRoleNot(Role role);

    /* Posição no ranking de xp: quantos participantes têm xp maior que o
       informado (a posição é essa contagem + 1) e o total de participantes,
       usados pelo card de nível em /participantes. */
    long countByRoleAndXpGreaterThan(Role role, Integer xp);
    long countByRole(Role role);

    /* Ranking completo (aba Ranking em /participantes): só quem já tem xp
       atribuído, do maior pro menor. Empate = posição sequencial (a posição
       final é calculada pelo índice na lista, não por essa query). */
    List<Pessoa> findByRoleAndXpIsNotNullOrderByXpDesc(Role role);

}
