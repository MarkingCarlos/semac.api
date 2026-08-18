package com.semac.java_api.controller;

import com.semac.java_api.dto.VariacaoCriarDTO;
import com.semac.java_api.dto.VariacaoItemRequestDTO;
import com.semac.java_api.dto.VariacaoRequestDTO;
import com.semac.java_api.dto.VariacaoResponseDTO;
import com.semac.java_api.model.Conjunto;
import com.semac.java_api.model.CotacaoFornecedor;
import com.semac.java_api.model.Variacao;
import com.semac.java_api.model.VariacaoItem;
import com.semac.java_api.repository.ConjuntoRepository;
import com.semac.java_api.repository.CotacaoFornecedorRepository;
import com.semac.java_api.repository.VariacaoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/* Uma variação é uma coluna da grade de comparação de um conjunto (ex.:
   "Coffee Cheio"). Vive à parte de /api/conjunto pra permitir editar as
   quantidades de uma variação sem reenviar o conjunto inteiro. */
@RestController
@RequestMapping("/api/variacao")
public class VariacaoController {

    private final VariacaoRepository variacaoRepository;
    private final ConjuntoRepository conjuntoRepository;
    private final CotacaoFornecedorRepository cotacaoFornecedorRepository;

    public VariacaoController(VariacaoRepository variacaoRepository,
                              ConjuntoRepository conjuntoRepository,
                              CotacaoFornecedorRepository cotacaoFornecedorRepository) {
        this.variacaoRepository = variacaoRepository;
        this.conjuntoRepository = conjuntoRepository;
        this.cotacaoFornecedorRepository = cotacaoFornecedorRepository;
    }

    @PostMapping
    public ResponseEntity<VariacaoResponseDTO> criar(@Valid @RequestBody VariacaoCriarDTO dto) {
        Conjunto conjunto = conjuntoRepository.findById(dto.conjuntoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conjunto inválido."));
        Variacao variacao = new Variacao();
        variacao.setConjunto(conjunto);
        variacao.setNome(dto.nome());
        Variacao salva = variacaoRepository.save(variacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(ConjuntoController.paraRespostaVariacao(salva));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VariacaoResponseDTO> atualizar(@PathVariable Integer id,
                                                         @Valid @RequestBody VariacaoRequestDTO dto) {
        return variacaoRepository.findById(id)
                .map(variacao -> {
                    aplicar(variacao, dto);
                    return ResponseEntity.ok(ConjuntoController.paraRespostaVariacao(variacaoRepository.save(variacao)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!variacaoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        variacaoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /* Mesma reconciliação usada em CotacaoController, agora chaveada por
       produto (cotacao_id) — cada variação tem no máximo uma linha por
       produto, com o fornecedor escolhido nela. Reaproveita a linha cujo
       produto continua na lista (só fornecedor/quantidade mudam) e só
       apaga/insere o que realmente mudou — evita esbarrar na constraint
       única (variacao_id, cotacao_id). */
    private void aplicar(Variacao variacao, VariacaoRequestDTO dto) {
        variacao.setNome(dto.nome());

        List<VariacaoItemRequestDTO> itensDto = dto.itens() != null ? dto.itens() : List.of();

        Map<Integer, VariacaoItem> existentesPorCotacao = variacao.getItens().stream()
                .collect(Collectors.toMap(item -> item.getCotacao().getId(), item -> item));

        List<VariacaoItem> resultado = new ArrayList<>();
        for (VariacaoItemRequestDTO itemDto : itensDto) {
            CotacaoFornecedor linhaCotada = cotacaoFornecedorRepository
                    .findByCotacaoIdAndFornecedorId(itemDto.cotacaoId(), itemDto.fornecedorId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Esse fornecedor não está cotado para esse produto."));

            VariacaoItem existente = existentesPorCotacao.remove(itemDto.cotacaoId());
            if (existente != null) {
                existente.setFornecedor(linhaCotada.getFornecedor());
                existente.setQuantidade(itemDto.quantidade());
                resultado.add(existente);
            } else {
                VariacaoItem novo = new VariacaoItem();
                novo.setVariacao(variacao);
                novo.setCotacao(linhaCotada.getCotacao());
                novo.setFornecedor(linhaCotada.getFornecedor());
                novo.setQuantidade(itemDto.quantidade());
                resultado.add(novo);
            }
        }

        variacao.getItens().clear();
        variacao.getItens().addAll(resultado);
    }
}
