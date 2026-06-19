package com.semac.java_api.controller;

import com.semac.java_api.dto.TipoInscricaoRequestDTO;
import com.semac.java_api.dto.TipoInscricaoResponseDTO;
import com.semac.java_api.exception.RecursoDuplicadoException;
import com.semac.java_api.model.TipoInscricao;
import com.semac.java_api.repository.TipoInscricaoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipo-inscricao")
public class TipoInscricaoController {

    private final TipoInscricaoRepository repository;

    public TipoInscricaoController(TipoInscricaoRepository repository) {
        this.repository = repository;
    }

    /* Lista os tipos de ingresso. Com ?ano= filtra pela edição (uso do
       /admin, que mostra o ano atual); sem o filtro, retorna todos. */
    @GetMapping
    public List<TipoInscricaoResponseDTO> listar(@RequestParam(required = false) Integer ano) {
        List<TipoInscricao> tipos = (ano != null)
                ? repository.findByAnoOrderByNomeAsc(ano)
                : repository.findAll();
        return tipos.stream().map(this::paraResposta).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoInscricaoResponseDTO> buscar(@PathVariable Integer id) {
        return repository.findById(id)
                .map(tipo -> ResponseEntity.ok(paraResposta(tipo)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TipoInscricaoResponseDTO> criar(@Valid @RequestBody TipoInscricaoRequestDTO dto) {
        if (repository.existsByNomeAndAno(dto.nome(), dto.ano())) {
            throw new RecursoDuplicadoException("Já existe um ingresso com esse nome neste ano.");
        }
        TipoInscricao tipo = new TipoInscricao();
        aplicar(tipo, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(repository.save(tipo)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoInscricaoResponseDTO> atualizar(@PathVariable Integer id,
                                                              @Valid @RequestBody TipoInscricaoRequestDTO dto) {
        return repository.findById(id)
                .map(tipo -> {
                    if (repository.existsByNomeAndAnoAndIdNot(dto.nome(), dto.ano(), id)) {
                        throw new RecursoDuplicadoException("Já existe um ingresso com esse nome neste ano.");
                    }
                    aplicar(tipo, dto);
                    return ResponseEntity.ok(paraResposta(repository.save(tipo)));
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

    private void aplicar(TipoInscricao tipo, TipoInscricaoRequestDTO dto) {
        tipo.setNome(dto.nome());
        tipo.setValor(dto.valor());
        tipo.setAno(dto.ano());
        tipo.setAtivo(dto.ativo() == null ? Boolean.TRUE : dto.ativo());
    }

    private TipoInscricaoResponseDTO paraResposta(TipoInscricao tipo) {
        return new TipoInscricaoResponseDTO(
                tipo.getId(), tipo.getNome(), tipo.getValor(), tipo.getAno(), tipo.getAtivo()
        );
    }
}
