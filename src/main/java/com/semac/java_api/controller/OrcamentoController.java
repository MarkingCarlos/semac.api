package com.semac.java_api.controller;

import com.semac.java_api.dto.OrcamentoResponseDTO;
import com.semac.java_api.model.Orcamento;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.repository.OrcamentoRepository;
import com.semac.java_api.repository.PalestranteRepository;
import com.semac.java_api.repository.PessoaRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Year;

/* Parâmetros do orçamento da edição vigente — SOMENTE LEITURA.

   Nada aqui é digitado: o teto vem do saldo da comissão e os três
   multiplicadores de escala vêm de contagens no banco. A rota existe
   para a interface poder mostrar de onde cada número sai; por isso não
   há PUT. */
@RestController
@RequestMapping("/api/orcamento")
public class OrcamentoController {

    private final OrcamentoRepository orcamentoRepository;
    private final PessoaRepository pessoaRepository;
    private final PalestranteRepository palestranteRepository;

    public OrcamentoController(OrcamentoRepository orcamentoRepository,
                               PessoaRepository pessoaRepository,
                               PalestranteRepository palestranteRepository) {
        this.orcamentoRepository = orcamentoRepository;
        this.pessoaRepository = pessoaRepository;
        this.palestranteRepository = palestranteRepository;
    }

    /* Derivado, não digitado: pessoas com role PARTICIPANTE (confirmadas)
       ou NULL (aguardando confirmação), sem quem comprou ingresso diário —
       esse não ganha kit. Vai na resposta só para a interface poder
       mostrar de onde sai a escala por inscrito. */
    private int inscritos() {
        return (int) pessoaRepository.contarInscritosComKit();
    }

    /* Participantes + pendentes, contando quem comprou ingresso diário —
       escala "por inscrito (inclui diária)". */
    private int inscritosTotais() {
        return (int) pessoaRepository.countByRoleIsNullOrRole(Role.PARTICIPANTE);
    }

    /* Comissão organizadora: role definido e diferente de PARTICIPANTE. */
    private int membrosComissao() {
        return (int) pessoaRepository.countByRoleNot(Role.PARTICIPANTE);
    }

    private int palestrantes() {
        return (int) palestranteRepository.count();
    }

    /* Sem orçamento cadastrado devolve um zerado (200) em vez de 404 —
       o dashboard precisa renderizar antes do primeiro cadastro. */
    @GetMapping
    public OrcamentoResponseDTO buscar() {
        return orcamentoRepository.findFirstByOrderByAnoDesc()
                .map(this::paraResposta)
                .orElseGet(() -> new OrcamentoResponseDTO(
                        null, Year.now().getValue(), inscritos(), inscritosTotais(), membrosComissao(), palestrantes()));
    }

    private OrcamentoResponseDTO paraResposta(Orcamento orcamento) {
        return new OrcamentoResponseDTO(
                orcamento.getId(),
                orcamento.getAno(),
                inscritos(),
                inscritosTotais(),
                membrosComissao(),
                palestrantes());
    }
}
