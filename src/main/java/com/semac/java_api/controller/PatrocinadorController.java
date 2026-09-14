package com.semac.java_api.controller;

import com.semac.java_api.dto.AtualizarStatusPagamentoDTO;
import com.semac.java_api.dto.CotaResponseDTO;
import com.semac.java_api.dto.PatrocinadorRequestDTO;
import com.semac.java_api.dto.PatrocinadorResponseDTO;
import com.semac.java_api.model.Cota;
import com.semac.java_api.model.Patrocinador;
import com.semac.java_api.model.enums.StatusPagamento;
import com.semac.java_api.repository.CotaRepository;
import com.semac.java_api.repository.PatrocinadorRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/patrocinador")
public class PatrocinadorController {

    private final PatrocinadorRepository patrocinadorRepository;
    private final CotaRepository cotaRepository;

    @Value("${app.upload.dir.logos}")
    private String uploadDirLogos;

    public PatrocinadorController(PatrocinadorRepository patrocinadorRepository,
                                  CotaRepository cotaRepository) {
        this.patrocinadorRepository = patrocinadorRepository;
        this.cotaRepository = cotaRepository;
    }

    @GetMapping
    public List<PatrocinadorResponseDTO> listar() {
        return patrocinadorRepository.findAllByOrderByNomeAsc().stream()
                .map(this::paraResposta)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatrocinadorResponseDTO> buscar(@PathVariable Integer id) {
        return patrocinadorRepository.findById(id)
                .map(p -> ResponseEntity.ok(paraResposta(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PatrocinadorResponseDTO> criar(@Valid @RequestBody PatrocinadorRequestDTO dto) {
        Patrocinador salvo = patrocinadorRepository.save(deRequisicao(new Patrocinador(), dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(salvo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatrocinadorResponseDTO> atualizar(
            @PathVariable Integer id,
            @Valid @RequestBody PatrocinadorRequestDTO dto
    ) {
        return patrocinadorRepository.findById(id)
                .map(existente -> ResponseEntity.ok(paraResposta(
                        patrocinadorRepository.save(deRequisicao(existente, dto)))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PatrocinadorResponseDTO> atualizarStatus(
            @PathVariable Integer id,
            @Valid @RequestBody AtualizarStatusPagamentoDTO dto
    ) {
        return patrocinadorRepository.findById(id)
                .map(existente -> {
                    existente.setStatusPagamento(dto.statusPagamento());
                    // Data de recebimento é gerenciada pelo servidor conforme o status.
                    if (dto.statusPagamento() == StatusPagamento.RECEBIDO) {
                        if (existente.getDataRecebimento() == null) {
                            existente.setDataRecebimento(LocalDateTime.now());
                        }
                    } else {
                        existente.setDataRecebimento(null);
                    }
                    return ResponseEntity.ok(paraResposta(patrocinadorRepository.save(existente)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!patrocinadorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        patrocinadorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /* Envia (ou substitui) a logo do patrocinador — usado pelo formulário
       de Finanças. Mesmo padrão de upload do comprovante de inscrição
       (InscricaoController), mas numa pasta própria (app.upload.dir.logos)
       já que a logo é pública, diferente do comprovante. */
    @PostMapping("/{id}/logo")
    public ResponseEntity<PatrocinadorResponseDTO> enviarLogo(
            @PathVariable Integer id,
            @RequestParam("arquivo") MultipartFile arquivo
    ) {
        Patrocinador patrocinador = patrocinadorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patrocinador não encontrado."));

        try {
            Path dir = Paths.get(uploadDirLogos);
            Files.createDirectories(dir);

            String original = arquivo.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.'))
                    : "";
            String nomeArquivo = "patrocinador-" + id + "_" + System.currentTimeMillis() + ext;

            arquivo.transferTo(dir.resolve(nomeArquivo));

            patrocinador.setLogoUrl(nomeArquivo);
            return ResponseEntity.ok(paraResposta(patrocinadorRepository.save(patrocinador)));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao salvar a logo.");
        }
    }

    /* Serve a logo do patrocinador — público, é usado direto num <img src>
       tanto no site quanto no preview do formulário de Finanças. */
    @GetMapping("/{id}/logo")
    public ResponseEntity<Resource> buscarLogo(@PathVariable Integer id) {
        Patrocinador patrocinador = patrocinadorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patrocinador não encontrado."));

        String nomeArquivo = patrocinador.getLogoUrl();
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Este patrocinador não tem logo.");
        }

        Path caminho = Paths.get(uploadDirLogos).resolve(nomeArquivo);
        Resource recurso;
        try {
            recurso = new UrlResource(caminho.toUri());
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao ler a logo.");
        }
        if (!recurso.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo da logo não encontrado.");
        }

        return ResponseEntity.ok()
                .contentType(mediaTypeDaLogo(nomeArquivo))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(recurso);
    }

    private MediaType mediaTypeDaLogo(String nomeArquivo) {
        String extensao = nomeArquivo.substring(nomeArquivo.lastIndexOf('.') + 1).toLowerCase();
        return switch (extensao) {
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "gif" -> MediaType.IMAGE_GIF;
            case "svg" -> MediaType.parseMediaType("image/svg+xml");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    /* ── Mapeamento ──────────────────────────────────────────────── */

    private Patrocinador deRequisicao(Patrocinador patrocinador, PatrocinadorRequestDTO dto) {
        Cota cota = cotaRepository.findById(dto.cotaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cota inválida."));

        BigDecimal desconto = dto.desconto() != null ? dto.desconto() : BigDecimal.ZERO;
        BigDecimal adicao = dto.adicao() != null ? dto.adicao() : BigDecimal.ZERO;

        patrocinador.setNome(dto.nome());
        patrocinador.setDescricao(dto.descricao());
        patrocinador.setLogoUrl(dto.logoUrl());
        patrocinador.setCota(cota);
        patrocinador.setDesconto(desconto);
        patrocinador.setAdicao(adicao);
        // valor_final é calculado pelo backend a partir da cota
        patrocinador.setValorFinal(cota.getValor().subtract(desconto).add(adicao));
        patrocinador.setStatusPagamento(
                dto.statusPagamento() != null ? dto.statusPagamento() : StatusPagamento.A_RECEBER);
        patrocinador.setDataRecebimento(dto.dataRecebimento());
        patrocinador.setObservacao(dto.observacao());
        return patrocinador;
    }

    private PatrocinadorResponseDTO paraResposta(Patrocinador p) {
        Cota cota = p.getCota();
        CotaResponseDTO cotaDTO = cota == null ? null
                : new CotaResponseDTO(cota.getId(), cota.getNivel(), cota.getValor());

        return new PatrocinadorResponseDTO(
                p.getId(),
                p.getNome(),
                p.getDescricao(),
                p.getLogoUrl(),
                cotaDTO,
                p.getDesconto(),
                p.getAdicao(),
                p.getValorFinal(),
                p.getStatusPagamento(),
                p.getDataRecebimento(),
                p.getObservacao()
        );
    }
}
