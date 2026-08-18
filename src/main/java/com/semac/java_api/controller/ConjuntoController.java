package com.semac.java_api.controller;

import com.semac.java_api.dto.ConjuntoRequestDTO;
import com.semac.java_api.dto.ConjuntoResponseDTO;
import com.semac.java_api.dto.VariacaoItemResponseDTO;
import com.semac.java_api.dto.VariacaoResponseDTO;
import com.semac.java_api.model.Conjunto;
import com.semac.java_api.model.Variacao;
import com.semac.java_api.repository.ConjuntoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conjunto")
public class ConjuntoController {

    private final ConjuntoRepository conjuntoRepository;

    public ConjuntoController(ConjuntoRepository conjuntoRepository) {
        this.conjuntoRepository = conjuntoRepository;
    }

    @GetMapping
    public List<ConjuntoResponseDTO> listar() {
        return conjuntoRepository.findAllByOrderByNomeAsc().stream()
                .map(ConjuntoController::paraResposta)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConjuntoResponseDTO> buscar(@PathVariable Integer id) {
        return conjuntoRepository.findById(id)
                .map(conjunto -> ResponseEntity.ok(paraResposta(conjunto)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ConjuntoResponseDTO> criar(@Valid @RequestBody ConjuntoRequestDTO dto) {
        Conjunto conjunto = new Conjunto();
        conjunto.setNome(dto.nome());
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(conjuntoRepository.save(conjunto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConjuntoResponseDTO> atualizar(@PathVariable Integer id,
                                                         @Valid @RequestBody ConjuntoRequestDTO dto) {
        return conjuntoRepository.findById(id)
                .map(conjunto -> {
                    conjunto.setNome(dto.nome());
                    return ResponseEntity.ok(paraResposta(conjuntoRepository.save(conjunto)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!conjuntoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        conjuntoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    static ConjuntoResponseDTO paraResposta(Conjunto conjunto) {
        List<VariacaoResponseDTO> variacoes = conjunto.getVariacoes().stream()
                .map(ConjuntoController::paraRespostaVariacao)
                .toList();
        return new ConjuntoResponseDTO(conjunto.getId(), conjunto.getNome(), variacoes);
    }

    static VariacaoResponseDTO paraRespostaVariacao(Variacao variacao) {
        List<VariacaoItemResponseDTO> itens = variacao.getItens().stream()
                .map(item -> new VariacaoItemResponseDTO(
                        item.getId(),
                        item.getCotacao().getId(),
                        item.getFornecedor().getId(),
                        item.getQuantidade()
                ))
                .toList();
        return new VariacaoResponseDTO(variacao.getId(), variacao.getConjunto().getId(), variacao.getNome(), itens);
    }
}
