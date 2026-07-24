package com.semac.java_api.controller;

import com.semac.java_api.dto.CotaRequestDTO;
import com.semac.java_api.dto.CotaResponseDTO;
import com.semac.java_api.exception.RecursoDuplicadoException;
import com.semac.java_api.model.Cota;
import com.semac.java_api.repository.CotaRepository;
import com.semac.java_api.repository.PatrocinadorRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/cota")
public class CotaController {

    private final CotaRepository repository;
    private final PatrocinadorRepository patrocinadorRepository;

    public CotaController(CotaRepository repository,
                          PatrocinadorRepository patrocinadorRepository) {
        this.repository = repository;
        this.patrocinadorRepository = patrocinadorRepository;
    }

    @GetMapping
    public List<CotaResponseDTO> listar() {
        return repository.findAllByOrderByValorAsc().stream()
                .map(this::paraResposta)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CotaResponseDTO> buscar(@PathVariable Integer id) {
        return repository.findById(id)
                .map(cota -> ResponseEntity.ok(paraResposta(cota)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CotaResponseDTO> criar(@Valid @RequestBody CotaRequestDTO dto) {
        if (repository.existsByNivel(dto.nivel())) {
            throw new RecursoDuplicadoException("Já existe uma cota para esse nível.");
        }
        Cota cota = new Cota();
        aplicar(cota, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(repository.save(cota)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CotaResponseDTO> atualizar(@PathVariable Integer id,
                                                     @Valid @RequestBody CotaRequestDTO dto) {
        return repository.findById(id)
                .map(cota -> {
                    if (repository.existsByNivelAndIdNot(dto.nivel(), id)) {
                        throw new RecursoDuplicadoException("Já existe uma cota para esse nível.");
                    }
                    aplicar(cota, dto);
                    return ResponseEntity.ok(paraResposta(repository.save(cota)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        /* Sem esta checagem a FK estoura como DataIntegrityViolationException
           e o handler global devolve "Já existe um registro com esses dados",
           que não faz sentido para uma exclusão. */
        if (patrocinadorRepository.existsByCotaId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Há patrocinadores vinculados a esta cota. Altere a cota deles antes de excluí-la.");
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void aplicar(Cota cota, CotaRequestDTO dto) {
        cota.setNivel(dto.nivel());
        cota.setValor(dto.valor());
    }

    private CotaResponseDTO paraResposta(Cota cota) {
        return new CotaResponseDTO(cota.getId(), cota.getNivel(), cota.getValor());
    }
}
