package com.semac.java_api.controller;

import com.semac.java_api.dto.CrachaDTO;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.repository.PalestranteRepository;
import com.semac.java_api.repository.PessoaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/* Lista de quem recebe crachá impresso: todas as pessoas (participantes
   confirmados, pendentes e comissão, exceto contas desativadas) e os
   palestrantes. A montagem e a impressão acontecem no navegador (aba
   Crachás do /admin).

   Devolve o uuid de check-in de todo mundo — por isso a rota é restrita
   no SecurityConfig; com o anyRequest().permitAll() de lá, sem a regra
   ela ficaria pública. */
@RestController
@RequestMapping("/api/cracha")
public class CrachaController {

    private final PessoaRepository pessoaRepository;
    private final PalestranteRepository palestranteRepository;

    public CrachaController(PessoaRepository pessoaRepository,
                            PalestranteRepository palestranteRepository) {
        this.pessoaRepository = pessoaRepository;
        this.palestranteRepository = palestranteRepository;
    }

    @GetMapping
    public List<CrachaDTO> listar() {
        /* Conta desativada no /admin não recebe crachá. */
        Stream<CrachaDTO> pessoas = pessoaRepository.findAll().stream()
                .filter(pessoa -> !Boolean.FALSE.equals(pessoa.getAtivo()))
                .map(pessoa -> new CrachaDTO(
                        pessoa.getId(),
                        pessoa.getNome(),
                        pessoa.getUuid(),
                        pessoa.getRole() == null || pessoa.getRole() == Role.PARTICIPANTE
                                ? "PARTICIPANTE"
                                : "COMISSAO"));

        Stream<CrachaDTO> palestrantes = palestranteRepository.findAll().stream()
                .map(palestrante -> new CrachaDTO(
                        palestrante.getId(),
                        palestrante.getNome(),
                        null,
                        "PALESTRANTE"));

        return Stream.concat(pessoas, palestrantes)
                .sorted(Comparator.comparing(CrachaDTO::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
