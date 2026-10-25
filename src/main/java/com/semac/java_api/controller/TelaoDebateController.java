package com.semac.java_api.controller;

import com.semac.java_api.dto.TelaoDebateRequestDTO;
import com.semac.java_api.dto.TelaoDebateResponseDTO;
import com.semac.java_api.service.TelaoDebateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/* Telão do debate controlado de qualquer aparelho (/debate/<id> no
   notebook ou no celular). O painel publica o estado a cada mudança; o
   telão e os demais painéis consultam a cada ~1s. Mesmo público do
   painel no frontend (diretoria e presidência, ver SecurityConfig). */
@RestController
@RequestMapping("/api/evento/{id}/telao-debate")
public class TelaoDebateController {

    private final TelaoDebateService telaoDebateService;

    public TelaoDebateController(TelaoDebateService telaoDebateService) {
        this.telaoDebateService = telaoDebateService;
    }

    /* `telao=true` só na guia projetada: marca que ela está aberta. */
    @GetMapping
    public TelaoDebateResponseDTO buscar(@PathVariable Integer id,
                                         @RequestParam(defaultValue = "false") boolean telao) {
        return telaoDebateService.buscar(id, telao);
    }

    /* Devolve o estado que ficou valendo: o enviado, ou o guardado se ele
       for mais novo (outro aparelho mexeu depois). */
    @PutMapping
    public TelaoDebateResponseDTO publicar(@PathVariable Integer id,
                                           @Valid @RequestBody TelaoDebateRequestDTO dto) {
        return telaoDebateService.publicar(id, dto.estado(), dto.alteradoEm());
    }
}
