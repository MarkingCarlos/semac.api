package com.semac.java_api.controller;

import com.semac.java_api.dto.CaixaFundunespRequestDTO;
import com.semac.java_api.dto.CaixaFundunespResponseDTO;
import com.semac.java_api.model.CaixaFundunesp;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.repository.CaixaFundunespRepository;
import com.semac.java_api.repository.PessoaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/* Saldo da FundoUnesp — registro único, editado na aba Resumo do
   módulo financeiro. Por ser linha única, a rota não tem /{id}: o GET
   devolve o registro (ou um zerado quando a tabela ainda está vazia) e
   o PUT atualiza o existente ou cria o primeiro. */
@RestController
@RequestMapping("/api/caixa-fundunesp")
public class CaixaFundunespController {

    private final CaixaFundunespRepository caixaRepository;
    private final PessoaRepository pessoaRepository;

    public CaixaFundunespController(CaixaFundunespRepository caixaRepository,
                                    PessoaRepository pessoaRepository) {
        this.caixaRepository = caixaRepository;
        this.pessoaRepository = pessoaRepository;
    }

    /* Tabela vazia devolve um registro zerado (200) em vez de 404 —
       o card do Resumo precisa renderizar antes do primeiro cadastro. */
    @GetMapping
    public CaixaFundunespResponseDTO buscar() {
        return caixaRepository.findFirstByOrderByIdAsc()
                .map(this::paraResposta)
                .orElseGet(() -> new CaixaFundunespResponseDTO(
                        null, BigDecimal.ZERO, null, null, null));
    }

    @PutMapping
    public CaixaFundunespResponseDTO atualizar(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody CaixaFundunespRequestDTO dto) {
        CaixaFundunesp caixa = caixaRepository.findFirstByOrderByIdAsc()
                .orElseGet(CaixaFundunesp::new);

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

    private CaixaFundunespResponseDTO paraResposta(CaixaFundunesp caixa) {
        Pessoa autor = caixa.getAtualizadoPor();
        return new CaixaFundunespResponseDTO(
                caixa.getId(),
                caixa.getValor(),
                caixa.getDataAtualizacao(),
                autor == null ? null : autor.getId(),
                autor == null ? null : autor.getNome()
        );
    }
}
