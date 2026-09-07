package com.semac.java_api.controller;

import com.semac.java_api.dto.TipoInscricaoRequestDTO;
import com.semac.java_api.dto.TipoInscricaoResponseDTO;
import com.semac.java_api.dto.VerificarCodigoIngressoDTO;
import com.semac.java_api.dto.VerificarCodigoIngressoRespostaDTO;
import com.semac.java_api.exception.RecursoDuplicadoException;
import com.semac.java_api.model.TipoInscricao;
import com.semac.java_api.repository.TipoInscricaoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    /* Verificação pública do código de acesso, usada pelo cadastro em
       /inscricoes para bloquear o avanço da etapa de ingresso antes do
       envio final (que revalida do mesmo jeito em InscricaoService —
       esta rota é só conveniência de UX, não é a barreira de segurança).
       Nunca revela o código real, só se bateu ou não. */
    @PostMapping("/{id}/verificar-codigo")
    public VerificarCodigoIngressoRespostaDTO verificarCodigo(@PathVariable Integer id,
                                                              @RequestBody VerificarCodigoIngressoDTO dto) {
        TipoInscricao tipo = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingresso não encontrado."));
        return new VerificarCodigoIngressoRespostaDTO(codigoBate(tipo, dto.codigo()));
    }

    private void aplicar(TipoInscricao tipo, TipoInscricaoRequestDTO dto) {
        tipo.setNome(dto.nome());
        tipo.setValor(dto.valor());
        tipo.setAno(dto.ano());
        tipo.setAtivo(dto.ativo() == null ? Boolean.TRUE : dto.ativo());
        tipo.setCamisetasGratis(dto.camisetasGratis() == null ? 0 : dto.camisetasGratis());

        // maxDias só faz sentido em ingresso de diária: fora disso é zerado
        // para não deixar resíduo quando o admin desmarca a opção.
        boolean porDia = Boolean.TRUE.equals(dto.porDia());
        tipo.setPorDia(porDia);
        tipo.setMaxDias(porDia ? dto.maxDias() : null);

        if (porDia && dto.maxDias() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o máximo de diárias do ingresso.");
        }

        // O código nunca volta numa resposta (ver TipoInscricaoResponseDTO),
        // então só mexe nele quando o front sinaliza que o campo foi
        // tocado — senão editar outro campo apagaria o código já salvo.
        if (Boolean.TRUE.equals(dto.alterarCodigo())) {
            String novoCodigo = dto.codigo() == null ? null : dto.codigo().trim();
            tipo.setCodigo(novoCodigo == null || novoCodigo.isBlank() ? null : novoCodigo);
        }
    }

    private boolean codigoBate(TipoInscricao tipo, String candidato) {
        if (tipo.getCodigo() == null || tipo.getCodigo().isBlank()) {
            return true;
        }
        return tipo.getCodigo().equals(candidato == null ? null : candidato.trim());
    }

    private TipoInscricaoResponseDTO paraResposta(TipoInscricao tipo) {
        boolean codigoDefinido = tipo.getCodigo() != null && !tipo.getCodigo().isBlank();
        return new TipoInscricaoResponseDTO(
                tipo.getId(), tipo.getNome(), tipo.getValor(), tipo.getAno(), tipo.getAtivo(),
                tipo.getCamisetasGratis(), tipo.getPorDia(), tipo.getMaxDias(), codigoDefinido
        );
    }
}
