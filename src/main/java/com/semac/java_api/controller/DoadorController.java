package com.semac.java_api.controller;

import com.semac.java_api.dto.DoadorRequestDTO;
import com.semac.java_api.dto.DoadorResponseDTO;
import com.semac.java_api.dto.ResumoDoacaoPublicoDTO;
import com.semac.java_api.model.Doador;
import com.semac.java_api.repository.DoadorRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doador")
public class DoadorController {

    private final DoadorRepository doadorRepository;

    public DoadorController(DoadorRepository doadorRepository) {
        this.doadorRepository = doadorRepository;
    }

    /* ── Administração (CRUD completo) ───────────────────────────── */

    @GetMapping
    public List<DoadorResponseDTO> listar() {
        return doadorRepository.findAllByOrderByDataDesc().stream()
                .map(this::paraResposta)
                .toList();
    }

    @PostMapping
    public ResponseEntity<DoadorResponseDTO> criar(@Valid @RequestBody DoadorRequestDTO dto) {
        Doador salvo = doadorRepository.save(deRequisicao(new Doador(), dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(salvo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DoadorResponseDTO> atualizar(
            @PathVariable Integer id,
            @Valid @RequestBody DoadorRequestDTO dto
    ) {
        return doadorRepository.findById(id)
                .map(existente -> ResponseEntity.ok(paraResposta(
                        doadorRepository.save(deRequisicao(existente, dto)))))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!doadorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        doadorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /* ── Página pública (sem valores individuais) ────────────────── */

    @GetMapping("/resumo-publico")
    public ResumoDoacaoPublicoDTO resumoPublico() {
        List<Doador> doadores = doadorRepository.findAllByOrderByDataDesc();

        BigDecimal totalArrecadado = doadores.stream()
                .map(Doador::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Lista já vem da mais recente para a mais antiga; manter o primeiro
        // registro de cada nome preserva a última doação e remove repetidos.
        Map<String, Boolean> vistos = new LinkedHashMap<>();
        for (Doador doador : doadores) {
            vistos.putIfAbsent(doador.getNome(), Boolean.TRUE);
        }

        return new ResumoDoacaoPublicoDTO(totalArrecadado, List.copyOf(vistos.keySet()));
    }

    /* ── Mapeamento ──────────────────────────────────────────────── */

    private Doador deRequisicao(Doador doador, DoadorRequestDTO dto) {
        doador.setNome(dto.nome());
        doador.setValor(dto.valor());
        doador.setData(dto.data());
        return doador;
    }

    private DoadorResponseDTO paraResposta(Doador doador) {
        return new DoadorResponseDTO(
                doador.getId(),
                doador.getNome(),
                doador.getValor(),
                doador.getData()
        );
    }
}
