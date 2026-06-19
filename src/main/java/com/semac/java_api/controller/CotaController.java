package com.semac.java_api.controller;

import com.semac.java_api.dto.CotaRequestDTO;
import com.semac.java_api.dto.CotaResponseDTO;
import com.semac.java_api.exception.RecursoDuplicadoException;
import com.semac.java_api.model.Cota;
import com.semac.java_api.repository.CotaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cota")
public class CotaController {

    private final CotaRepository repository;

    public CotaController(CotaRepository repository) {
        this.repository = repository;
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
