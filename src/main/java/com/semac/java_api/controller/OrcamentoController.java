package com.semac.java_api.controller;

import com.semac.java_api.dto.OrcamentoRequestDTO;
import com.semac.java_api.dto.OrcamentoResponseDTO;
import com.semac.java_api.model.Orcamento;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.repository.OrcamentoRepository;
import com.semac.java_api.repository.PessoaRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.Year;

/* Parâmetros do orçamento da edição vigente: os contadores que
   alimentam a escala das previsões. O teto não vive aqui — é derivado
   do saldo da conta da comissão (ver PrevisaoService.resumo).

   Sempre opera sobre a edição mais recente — por isso a rota não tem
   /{id}. Ano ainda não cadastrado é criado no primeiro PUT. */
@RestController
@RequestMapping("/api/orcamento")
public class OrcamentoController {

    private final OrcamentoRepository orcamentoRepository;
    private final PessoaRepository pessoaRepository;

    public OrcamentoController(OrcamentoRepository orcamentoRepository,
                               PessoaRepository pessoaRepository) {
        this.orcamentoRepository = orcamentoRepository;
        this.pessoaRepository = pessoaRepository;
    }

    /* Derivado, não digitado: pessoas com role PARTICIPANTE (confirmadas)
       ou NULL (aguardando confirmação). Vai na resposta só para a
       interface poder mostrar de onde sai a escala por inscrito. */
    private int inscritos() {
        return (int) pessoaRepository.countByRoleIsNullOrRole(Role.PARTICIPANTE);
    }

    /* Sem orçamento cadastrado devolve um zerado (200) em vez de 404 —
       o dashboard precisa renderizar antes do primeiro cadastro. */
    @GetMapping
    public OrcamentoResponseDTO buscar() {
        return orcamentoRepository.findFirstByOrderByAnoDesc()
                .map(this::paraResposta)
                .orElseGet(() -> new OrcamentoResponseDTO(
                        null, Year.now().getValue(), inscritos(), 0, 0));
    }

    @PutMapping
    public OrcamentoResponseDTO atualizar(@Valid @RequestBody OrcamentoRequestDTO dto) {
        Orcamento orcamento = orcamentoRepository.findFirstByOrderByAnoDesc()
                .orElseGet(() -> {
                    Orcamento novo = new Orcamento();
                    novo.setAno(Year.now().getValue());
                    return novo;
                });

        orcamento.setMembrosComissao(dto.membrosComissao());
        orcamento.setPalestrantesPrevistos(dto.palestrantesPrevistos());

        return paraResposta(orcamentoRepository.save(orcamento));
    }

    private OrcamentoResponseDTO paraResposta(Orcamento orcamento) {
        return new OrcamentoResponseDTO(
                orcamento.getId(),
                orcamento.getAno(),
                inscritos(),
                orcamento.getMembrosComissao(),
                orcamento.getPalestrantesPrevistos());
    }
}
