package com.semac.java_api.controller;

import com.semac.java_api.dto.TrilhaRequestDTO;
import com.semac.java_api.dto.TrilhaResponseDTO;
import com.semac.java_api.exception.RecursoDuplicadoException;
import com.semac.java_api.model.Trilha;
import com.semac.java_api.repository.TrilhaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

/* CRUD das trilhas temáticas (tabela `trilha`). O GET segue aberto de
   propósito — alimenta o filtro da programação pública, diferente de
   tipo-evento (só usado no /admin); escrita é exclusiva da comissão (ver
   SecurityConfig). Excluir uma trilha com eventos vinculados é barrado
   pela FK (DataIntegrityViolationException → 409 no GlobalExceptionHandler). */
@RestController
@RequestMapping("/api/trilha")
public class TrilhaController {

    private final TrilhaRepository trilhaRepository;

    public TrilhaController(TrilhaRepository trilhaRepository) {
        this.trilhaRepository = trilhaRepository;
    }

    @GetMapping
    public List<TrilhaResponseDTO> listar() {
        return trilhaRepository.findAll().stream()
                .sorted(Comparator.comparing(t -> t.getNome().toLowerCase()))
                .map(this::paraResposta)
                .toList();
    }

    @PostMapping
    public ResponseEntity<TrilhaResponseDTO> criar(@Valid @RequestBody TrilhaRequestDTO dto) {
        if (trilhaRepository.existsByNomeIgnoreCase(dto.nome())) {
            throw new RecursoDuplicadoException("Já existe uma trilha com esse nome.");
        }
        Trilha trilha = new Trilha();
        trilha.setNome(dto.nome());
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(trilhaRepository.save(trilha)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrilhaResponseDTO> atualizar(@PathVariable Integer id,
                                                        @Valid @RequestBody TrilhaRequestDTO dto) {
        return trilhaRepository.findById(id)
                .map(trilha -> {
                    if (trilhaRepository.existsByNomeIgnoreCaseAndIdNot(dto.nome(), id)) {
                        throw new RecursoDuplicadoException("Já existe uma trilha com esse nome.");
                    }
                    trilha.setNome(dto.nome());
                    return ResponseEntity.ok(paraResposta(trilhaRepository.save(trilha)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!trilhaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        trilhaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private TrilhaResponseDTO paraResposta(Trilha trilha) {
        return new TrilhaResponseDTO(trilha.getId(), trilha.getNome());
    }
}
