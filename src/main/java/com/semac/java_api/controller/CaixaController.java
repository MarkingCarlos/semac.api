package com.semac.java_api.controller;

import com.semac.java_api.dto.CaixaRequestDTO;
import com.semac.java_api.dto.CaixaResponseDTO;
import com.semac.java_api.model.Caixa;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.enums.ContaFinanceira;
import com.semac.java_api.repository.CaixaRepository;
import com.semac.java_api.repository.PessoaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/* Saldo inicial de cada conta, editado na aba Resumo do módulo
   financeiro. Uma linha por conta: a rota é endereçada pela conta
   (/api/caixa/COMISSAO), não por id. */
@RestController
@RequestMapping("/api/caixa")
public class CaixaController {

    private final CaixaRepository caixaRepository;
    private final PessoaRepository pessoaRepository;

    public CaixaController(CaixaRepository caixaRepository,
                           PessoaRepository pessoaRepository) {
        this.caixaRepository = caixaRepository;
        this.pessoaRepository = pessoaRepository;
    }

    @GetMapping
    public List<CaixaResponseDTO> listar() {
        return caixaRepository.findAllByOrderByContaAsc().stream()
                .map(this::paraResposta)
                .toList();
    }

    /* Conta ainda sem linha devolve um registro zerado (200) em vez de
       404 — o card do Resumo precisa renderizar antes do primeiro
       cadastro. */
    @GetMapping("/{conta}")
    public CaixaResponseDTO buscar(@PathVariable ContaFinanceira conta) {
        return caixaRepository.findByConta(conta)
                .map(this::paraResposta)
                .orElseGet(() -> new CaixaResponseDTO(
                        null, BigDecimal.ZERO, conta.name(), null, null, null));
    }

    @PutMapping("/{conta}")
    public CaixaResponseDTO atualizar(@PathVariable ContaFinanceira conta,
                                      @AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody CaixaRequestDTO dto) {
        Caixa caixa = caixaRepository.findByConta(conta)
                .orElseGet(() -> {
                    Caixa nova = new Caixa();
                    nova.setConta(conta);
                    return nova;
                });

        caixa.setValor(dto.valor());
        caixa.setDataAtualizacao(LocalDateTime.now());
        caixa.setAtualizadoPor(pessoaDoToken(jwt));

        return paraResposta(caixaRepository.save(caixa));
    }

    /* ── Mapeamento ──────────────────────────────────────────────── */

    /* Identifica o autor do ajuste pela claim `id` do token — nunca por
       parâmetro (mesmo critério de PessoaController.idDoToken). */
    private Pessoa pessoaDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (!(id instanceof Number numero)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
        }
        return pessoaRepository.findById(numero.intValue())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    }

    private CaixaResponseDTO paraResposta(Caixa caixa) {
        Pessoa autor = caixa.getAtualizadoPor();
        return new CaixaResponseDTO(
                caixa.getId(),
                caixa.getValor(),
                caixa.getConta().name(),
                caixa.getDataAtualizacao(),
                autor == null ? null : autor.getId(),
                autor == null ? null : autor.getNome()
        );
    }
}
