package com.semac.java_api.controller;

import com.semac.java_api.dto.NivelRequestDTO;
import com.semac.java_api.dto.NivelResponseDTO;
import com.semac.java_api.exception.RecursoDuplicadoException;
import com.semac.java_api.model.Nivel;
import com.semac.java_api.repository.NivelRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

/* CRUD dos níveis de participante (tabela `nivel`): nome + xp mínimo.
   Gerenciado na aba Informações SEMAC do /admin (escrita restrita a
   DIRETOR_SITE/PRESIDENTE, ver SecurityConfig). Excluir um nível com
   participantes vinculados é barrado pela FK (DataIntegrityViolationException
   → 409 no GlobalExceptionHandler). */
@RestController
@RequestMapping("/api/nivel")
public class NivelController {

    private final NivelRepository nivelRepository;

    public NivelController(NivelRepository nivelRepository) {
        this.nivelRepository = nivelRepository;
    }

    @GetMapping
    public List<NivelResponseDTO> listar() {
        return nivelRepository.findAll().stream()
                .sorted(Comparator.comparing(Nivel::getXpMinimo))
                .map(this::paraResposta)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<NivelResponseDTO> buscar(@PathVariable Integer id) {
        return nivelRepository.findById(id)
                .map(nivel -> ResponseEntity.ok(paraResposta(nivel)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<NivelResponseDTO> criar(@Valid @RequestBody NivelRequestDTO dto) {
        if (nivelRepository.existsByNomeIgnoreCase(dto.nome())) {
            throw new RecursoDuplicadoException("Já existe um nível com esse nome.");
        }
        if (nivelRepository.existsByXpMinimo(dto.xpMinimo())) {
            throw new RecursoDuplicadoException("Já existe um nível com esse xp mínimo.");
        }
        Nivel nivel = new Nivel();
        aplicar(nivel, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(nivelRepository.save(nivel)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NivelResponseDTO> atualizar(@PathVariable Integer id,
                                                       @Valid @RequestBody NivelRequestDTO dto) {
        return nivelRepository.findById(id)
                .map(nivel -> {
                    if (nivelRepository.existsByNomeIgnoreCaseAndIdNot(dto.nome(), id)) {
                        throw new RecursoDuplicadoException("Já existe um nível com esse nome.");
                    }
                    if (nivelRepository.existsByXpMinimoAndIdNot(dto.xpMinimo(), id)) {
                        throw new RecursoDuplicadoException("Já existe um nível com esse xp mínimo.");
                    }
                    aplicar(nivel, dto);
                    return ResponseEntity.ok(paraResposta(nivelRepository.save(nivel)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!nivelRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        nivelRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void aplicar(Nivel nivel, NivelRequestDTO dto) {
        nivel.setNome(dto.nome());
        nivel.setXpMinimo(dto.xpMinimo());
    }

    private NivelResponseDTO paraResposta(Nivel nivel) {
        return new NivelResponseDTO(nivel.getId(), nivel.getNome(), nivel.getXpMinimo());
    }
}
