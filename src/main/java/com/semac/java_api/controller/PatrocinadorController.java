package com.semac.java_api.controller;

import com.semac.java_api.dto.AtualizarStatusPagamentoDTO;
import com.semac.java_api.dto.CotaResponseDTO;
import com.semac.java_api.dto.PatrocinadorRequestDTO;
import com.semac.java_api.dto.PatrocinadorResponseDTO;
import com.semac.java_api.model.Cota;
import com.semac.java_api.model.Patrocinador;
import com.semac.java_api.model.enums.StatusPagamento;
import com.semac.java_api.repository.CotaRepository;
import com.semac.java_api.repository.PatrocinadorRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/patrocinador")
public class PatrocinadorController {

    private final PatrocinadorRepository patrocinadorRepository;
    private final CotaRepository cotaRepository;

    public PatrocinadorController(PatrocinadorRepository patrocinadorRepository,
                                  CotaRepository cotaRepository) {
        this.patrocinadorRepository = patrocinadorRepository;
        this.cotaRepository = cotaRepository;
    }

    @GetMapping
    public List<PatrocinadorResponseDTO> listar() {
        return patrocinadorRepository.findAllByOrderByNomeAsc().stream()
                .map(this::paraResposta)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatrocinadorResponseDTO> buscar(@PathVariable Integer id) {
        return patrocinadorRepository.findById(id)
                .map(p -> ResponseEntity.ok(paraResposta(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PatrocinadorResponseDTO> criar(@Valid @RequestBody PatrocinadorRequestDTO dto) {
        Patrocinador salvo = patrocinadorRepository.save(deRequisicao(new Patrocinador(), dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(salvo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatrocinadorResponseDTO> atualizar(
            @PathVariable Integer id,
            @Valid @RequestBody PatrocinadorRequestDTO dto
    ) {
        return patrocinadorRepository.findById(id)
                .map(existente -> ResponseEntity.ok(paraResposta(
                        patrocinadorRepository.save(deRequisicao(existente, dto)))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PatrocinadorResponseDTO> atualizarStatus(
            @PathVariable Integer id,
            @Valid @RequestBody AtualizarStatusPagamentoDTO dto
    ) {
        return patrocinadorRepository.findById(id)
                .map(existente -> {
                    existente.setStatusPagamento(dto.statusPagamento());
                    // Data de recebimento é gerenciada pelo servidor conforme o status.
                    if (dto.statusPagamento() == StatusPagamento.RECEBIDO) {
                        if (existente.getDataRecebimento() == null) {
                            existente.setDataRecebimento(LocalDateTime.now());
                        }
                    } else {
                        existente.setDataRecebimento(null);
                    }
                    return ResponseEntity.ok(paraResposta(patrocinadorRepository.save(existente)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!patrocinadorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        patrocinadorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /* ── Mapeamento ──────────────────────────────────────────────── */

    private Patrocinador deRequisicao(Patrocinador patrocinador, PatrocinadorRequestDTO dto) {
        Cota cota = cotaRepository.findById(dto.cotaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cota inválida."));

        BigDecimal desconto = dto.desconto() != null ? dto.desconto() : BigDecimal.ZERO;
        BigDecimal adicao = dto.adicao() != null ? dto.adicao() : BigDecimal.ZERO;

        patrocinador.setNome(dto.nome());
        patrocinador.setDescricao(dto.descricao());
        patrocinador.setLogoUrl(dto.logoUrl());
        patrocinador.setCota(cota);
        patrocinador.setDesconto(desconto);
        patrocinador.setAdicao(adicao);
        // valor_final é calculado pelo backend a partir da cota
        patrocinador.setValorFinal(cota.getValor().subtract(desconto).add(adicao));
        patrocinador.setStatusPagamento(
                dto.statusPagamento() != null ? dto.statusPagamento() : StatusPagamento.A_RECEBER);
        patrocinador.setDataRecebimento(dto.dataRecebimento());
        patrocinador.setObservacao(dto.observacao());
        return patrocinador;
    }

    private PatrocinadorResponseDTO paraResposta(Patrocinador p) {
        Cota cota = p.getCota();
        CotaResponseDTO cotaDTO = cota == null ? null
                : new CotaResponseDTO(cota.getId(), cota.getNivel(), cota.getValor());

        return new PatrocinadorResponseDTO(
                p.getId(),
                p.getNome(),
                p.getDescricao(),
                p.getLogoUrl(),
                cotaDTO,
                p.getDesconto(),
                p.getAdicao(),
                p.getValorFinal(),
                p.getStatusPagamento(),
                p.getDataRecebimento(),
                p.getObservacao()
        );
    }
}
