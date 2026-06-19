package com.semac.java_api.controller;

import com.semac.java_api.dto.TipoEventoRequestDTO;
import com.semac.java_api.dto.TipoEventoResponseDTO;
import com.semac.java_api.exception.RecursoDuplicadoException;
import com.semac.java_api.model.TipoEvento;
import com.semac.java_api.repository.TipoEventoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

/* CRUD dos tipos de evento (tabela `tipo_evento`). Alimenta o seletor de
   tipo no formulário de evento e é gerenciado na aba Conteúdo do /admin.
   Excluir um tipo com eventos vinculados é barrado pela FK
   (DataIntegrityViolationException → 409 no GlobalExceptionHandler). */
@RestController
@RequestMapping("/api/tipo-evento")
public class TipoEventoController {

    private final TipoEventoRepository tipoEventoRepository;

    public TipoEventoController(TipoEventoRepository tipoEventoRepository) {
        this.tipoEventoRepository = tipoEventoRepository;
    }

    @GetMapping
    public List<TipoEventoResponseDTO> listar() {
        return tipoEventoRepository.findAll().stream()
                .sorted(Comparator.comparing(t -> t.getNome().toLowerCase()))
                .map(this::paraResposta)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoEventoResponseDTO> buscar(@PathVariable Integer id) {
        return tipoEventoRepository.findById(id)
                .map(tipo -> ResponseEntity.ok(paraResposta(tipo)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TipoEventoResponseDTO> criar(@Valid @RequestBody TipoEventoRequestDTO dto) {
        if (tipoEventoRepository.existsByNomeIgnoreCase(dto.nome())) {
            throw new RecursoDuplicadoException("Já existe um tipo de evento com esse nome.");
        }
        TipoEvento tipo = new TipoEvento();
        aplicar(tipo, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(tipoEventoRepository.save(tipo)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoEventoResponseDTO> atualizar(@PathVariable Integer id,
                                                           @Valid @RequestBody TipoEventoRequestDTO dto) {
        return tipoEventoRepository.findById(id)
                .map(tipo -> {
                    if (tipoEventoRepository.existsByNomeIgnoreCaseAndIdNot(dto.nome(), id)) {
                        throw new RecursoDuplicadoException("Já existe um tipo de evento com esse nome.");
                    }
                    aplicar(tipo, dto);
                    return ResponseEntity.ok(paraResposta(tipoEventoRepository.save(tipo)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!tipoEventoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        tipoEventoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void aplicar(TipoEvento tipo, TipoEventoRequestDTO dto) {
        tipo.setNome(dto.nome());
        tipo.setPontos(dto.pontos());
    }

    private TipoEventoResponseDTO paraResposta(TipoEvento tipo) {
        return new TipoEventoResponseDTO(tipo.getId(), tipo.getNome(), tipo.getPontos());
    }
}
