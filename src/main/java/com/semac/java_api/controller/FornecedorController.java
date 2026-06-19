package com.semac.java_api.controller;

import com.semac.java_api.dto.FornecedorRequestDTO;
import com.semac.java_api.dto.FornecedorResponseDTO;
import com.semac.java_api.model.Fornecedor;
import com.semac.java_api.repository.FornecedorRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fornecedor")
public class FornecedorController {

    private final FornecedorRepository fornecedorRepository;

    public FornecedorController(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
    }

    @GetMapping
    public List<FornecedorResponseDTO> listar() {
        return fornecedorRepository.findAllByOrderByNomeAsc().stream()
                .map(this::paraResposta)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FornecedorResponseDTO> buscar(@PathVariable Integer id) {
        return fornecedorRepository.findById(id)
                .map(f -> ResponseEntity.ok(paraResposta(f)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FornecedorResponseDTO> criar(@Valid @RequestBody FornecedorRequestDTO dto) {
        Fornecedor salvo = fornecedorRepository.save(deRequisicao(new Fornecedor(), dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(salvo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FornecedorResponseDTO> atualizar(
            @PathVariable Integer id,
            @Valid @RequestBody FornecedorRequestDTO dto
    ) {
        return fornecedorRepository.findById(id)
                .map(existente -> ResponseEntity.ok(paraResposta(
                        fornecedorRepository.save(deRequisicao(existente, dto)))))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!fornecedorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        fornecedorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /* ── Mapeamento ──────────────────────────────────────────────── */

    private Fornecedor deRequisicao(Fornecedor fornecedor, FornecedorRequestDTO dto) {
        fornecedor.setNome(dto.nome());
        fornecedor.setContato(dto.contato());
        fornecedor.setObservacao(dto.observacao());
        return fornecedor;
    }

    private FornecedorResponseDTO paraResposta(Fornecedor f) {
        return new FornecedorResponseDTO(f.getId(), f.getNome(), f.getContato(), f.getObservacao());
    }
}
