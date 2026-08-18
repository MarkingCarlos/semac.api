package com.semac.java_api.controller;

import com.semac.java_api.dto.CotacaoFornecedorRequestDTO;
import com.semac.java_api.dto.CotacaoFornecedorResponseDTO;
import com.semac.java_api.dto.CotacaoRequestDTO;
import com.semac.java_api.dto.CotacaoResponseDTO;
import com.semac.java_api.model.Cotacao;
import com.semac.java_api.model.CotacaoFornecedor;
import com.semac.java_api.model.Fornecedor;
import com.semac.java_api.repository.CotacaoRepository;
import com.semac.java_api.repository.FornecedorRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cotacao")
public class CotacaoController {

    private final CotacaoRepository cotacaoRepository;
    private final FornecedorRepository fornecedorRepository;

    public CotacaoController(CotacaoRepository cotacaoRepository,
                             FornecedorRepository fornecedorRepository) {
        this.cotacaoRepository = cotacaoRepository;
        this.fornecedorRepository = fornecedorRepository;
    }

    @GetMapping
    public List<CotacaoResponseDTO> listar() {
        return cotacaoRepository.findAllByOrderByDescricaoAsc().stream()
                .map(this::paraResposta)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CotacaoResponseDTO> buscar(@PathVariable Integer id) {
        return cotacaoRepository.findById(id)
                .map(cotacao -> ResponseEntity.ok(paraResposta(cotacao)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CotacaoResponseDTO> criar(@Valid @RequestBody CotacaoRequestDTO dto) {
        Cotacao cotacao = new Cotacao();
        aplicar(cotacao, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(cotacaoRepository.save(cotacao)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CotacaoResponseDTO> atualizar(@PathVariable Integer id,
                                                        @Valid @RequestBody CotacaoRequestDTO dto) {
        return cotacaoRepository.findById(id)
                .map(cotacao -> {
                    aplicar(cotacao, dto);
                    return ResponseEntity.ok(paraResposta(cotacaoRepository.save(cotacao)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!cotacaoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        cotacaoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /* Reconcilia a lista de fornecedores em vez de apagar tudo e recriar:
       linhas cujo fornecedor continua na lista são reaproveitadas (só o
       valorUnitario muda); só as removidas são apagadas (orphanRemoval) e
       só as realmente novas são inseridas. Isso evita que o Hibernate
       tente inserir uma linha nova antes de apagar a antiga e esbarre na
       constraint única (cotacao_id, fornecedor_id) — o que acontecia até
       ao editar só a categoria, já que a lista inteira era recriada com
       os mesmos fornecedores. */
    private void aplicar(Cotacao cotacao, CotacaoRequestDTO dto) {
        cotacao.setDescricao(dto.descricao());
        cotacao.setCategoria(dto.categoria());
        cotacao.setQuantidade(dto.quantidade());

        Map<Integer, CotacaoFornecedor> existentesPorFornecedor = cotacao.getFornecedores().stream()
                .collect(Collectors.toMap(linha -> linha.getFornecedor().getId(), linha -> linha));

        List<CotacaoFornecedor> resultado = new ArrayList<>();
        for (CotacaoFornecedorRequestDTO linhaDto : dto.fornecedores()) {
            CotacaoFornecedor existente = existentesPorFornecedor.remove(linhaDto.fornecedorId());
            if (existente != null) {
                existente.setValorUnitario(linhaDto.valorUnitario());
                resultado.add(existente);
            } else {
                Fornecedor fornecedor = fornecedorRepository.findById(linhaDto.fornecedorId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Fornecedor inválido."));
                CotacaoFornecedor nova = new CotacaoFornecedor();
                nova.setCotacao(cotacao);
                nova.setFornecedor(fornecedor);
                nova.setValorUnitario(linhaDto.valorUnitario());
                resultado.add(nova);
            }
        }

        cotacao.getFornecedores().clear();
        cotacao.getFornecedores().addAll(resultado);
    }

    private CotacaoResponseDTO paraResposta(Cotacao cotacao) {
        List<CotacaoFornecedorResponseDTO> fornecedores = cotacao.getFornecedores().stream()
                .map(linha -> new CotacaoFornecedorResponseDTO(
                        linha.getId(),
                        linha.getFornecedor().getId(),
                        linha.getValorUnitario()
                ))
                .toList();
        return new CotacaoResponseDTO(
                cotacao.getId(),
                cotacao.getDescricao(),
                cotacao.getCategoria(),
                cotacao.getQuantidade(),
                fornecedores
        );
    }
}
