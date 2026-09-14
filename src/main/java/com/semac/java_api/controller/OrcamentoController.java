package com.semac.java_api.controller;

import com.semac.java_api.dto.OrcamentoRequestDTO;
import com.semac.java_api.dto.OrcamentoResponseDTO;
import com.semac.java_api.model.Orcamento;
import com.semac.java_api.repository.OrcamentoRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Year;

/* Parâmetros do orçamento da edição vigente: o teto de gastos e os
   contadores que alimentam a escala das previsões.

   Sempre opera sobre a edição mais recente — por isso a rota não tem
   /{id}. Ano ainda não cadastrado é criado no primeiro PUT. */
@RestController
@RequestMapping("/api/orcamento")
public class OrcamentoController {

    private final OrcamentoRepository orcamentoRepository;

    public OrcamentoController(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    /* Sem orçamento cadastrado devolve um zerado (200) em vez de 404 —
       o dashboard precisa renderizar antes do primeiro cadastro. */
    @GetMapping
    public OrcamentoResponseDTO buscar() {
        return orcamentoRepository.findFirstByOrderByAnoDesc()
                .map(this::paraResposta)
                .orElseGet(() -> new OrcamentoResponseDTO(
                        null, Year.now().getValue(), BigDecimal.ZERO, 0, 0, 0));
    }

    @PutMapping
    public OrcamentoResponseDTO atualizar(@Valid @RequestBody OrcamentoRequestDTO dto) {
        Orcamento orcamento = orcamentoRepository.findFirstByOrderByAnoDesc()
                .orElseGet(() -> {
                    Orcamento novo = new Orcamento();
                    novo.setAno(Year.now().getValue());
                    return novo;
                });

        orcamento.setTeto(dto.teto());
        orcamento.setInscritosPrevistos(dto.inscritosPrevistos());
        orcamento.setMembrosComissao(dto.membrosComissao());
        orcamento.setPalestrantesPrevistos(dto.palestrantesPrevistos());

        return paraResposta(orcamentoRepository.save(orcamento));
    }

    private OrcamentoResponseDTO paraResposta(Orcamento orcamento) {
        return new OrcamentoResponseDTO(
                orcamento.getId(),
                orcamento.getAno(),
                orcamento.getTeto(),
                orcamento.getInscritosPrevistos(),
                orcamento.getMembrosComissao(),
                orcamento.getPalestrantesPrevistos());
    }
}
