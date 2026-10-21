package com.semac.java_api.controller;

import com.semac.java_api.service.PalestranteFotoService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/* Foto do palestrante, mostrada no telão do debate. O resto do palestrante
   (nome, cargo, descrição) é gravado junto com o evento — ver
   EventoService.sincronizarPalestrantes. O envio é dos mesmos papéis que
   editam evento; a leitura é pública, porque vai direto num <img src> —
   ver SecurityConfig. */
@RestController
@RequestMapping("/api/palestrante")
public class PalestranteController {

    private final PalestranteFotoService palestranteFotoService;

    public PalestranteController(PalestranteFotoService palestranteFotoService) {
        this.palestranteFotoService = palestranteFotoService;
    }

    @PostMapping("/{id}/foto")
    public ResponseEntity<Void> enviarFoto(@PathVariable Integer id,
                                           @RequestParam("arquivo") MultipartFile arquivo) {
        palestranteFotoService.salvar(id, arquivo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<Resource> buscarFoto(@PathVariable Integer id) {
        Resource recurso = palestranteFotoService.ler(id);
        return ResponseEntity.ok()
                .contentType(palestranteFotoService.tipoDaFoto(recurso))
                // A URL é a mesma depois de trocar a foto: sem cache longo,
                // senão o telão mostraria a antiga por um dia.
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                // Mesmo motivo do ConquistaController: rota pública, o
                // navegador não deve reinterpretar o arquivo como outro tipo.
                .header("X-Content-Type-Options", "nosniff")
                .body(recurso);
    }
}
