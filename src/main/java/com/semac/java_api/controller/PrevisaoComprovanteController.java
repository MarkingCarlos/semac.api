package com.semac.java_api.controller;

import com.semac.java_api.dto.ComprovantePrevisaoResponseDTO;
import com.semac.java_api.model.PrevisaoItemComprovante;
import com.semac.java_api.service.PrevisaoComprovanteService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/* Comprovantes de compra de um item da previsão.

   Fica sob /api/previsao/**, então o SecurityConfig já restringe tudo a
   PAPEIS_FINANCEIRO — diretores com leitura da Previsão não veem nem
   baixam os comprovantes.

   Um arquivo por requisição: o limite de multipart (6 MB por request) é
   global e vale também para as rotas públicas, então não foi aumentado.
   A interface envia vários arquivos em sequência e mostra o erro de cada
   um separadamente. */
@RestController
@RequestMapping("/api/previsao/{itemId}/comprovantes")
public class PrevisaoComprovanteController {

    private final PrevisaoComprovanteService comprovanteService;

    public PrevisaoComprovanteController(PrevisaoComprovanteService comprovanteService) {
        this.comprovanteService = comprovanteService;
    }

    @GetMapping
    public List<ComprovantePrevisaoResponseDTO> listar(@PathVariable Integer itemId) {
        return comprovanteService.listar(itemId);
    }

    @PostMapping
    public ResponseEntity<ComprovantePrevisaoResponseDTO> anexar(@PathVariable Integer itemId,
                                                                 @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comprovanteService.anexar(itemId, arquivo));
    }

    @GetMapping("/{comprovanteId}")
    public ResponseEntity<Resource> baixar(@PathVariable Integer itemId, @PathVariable Integer comprovanteId) {
        PrevisaoItemComprovante comprovante = comprovanteService.buscarOuFalhar(itemId, comprovanteId);
        Resource recurso = comprovanteService.lerArquivo(comprovante);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(comprovante.getTipoConteudo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(comprovante.getNomeOriginal(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                // Mesmo motivo do ConquistaController: o navegador não
                // deve reinterpretar o arquivo como outro tipo.
                .header("X-Content-Type-Options", "nosniff")
                .body(recurso);
    }

    @DeleteMapping("/{comprovanteId}")
    public ResponseEntity<Void> excluir(@PathVariable Integer itemId, @PathVariable Integer comprovanteId) {
        comprovanteService.excluir(itemId, comprovanteId);
        return ResponseEntity.noContent().build();
    }
}
