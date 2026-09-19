package com.semac.java_api.controller;

import com.semac.java_api.dto.RegraXpRequestDTO;
import com.semac.java_api.dto.RegraXpResponseDTO;
import com.semac.java_api.service.RegraXpService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/* Regras de xp: quanto vale cada presença, o acerto do Termo e os
   limites de atraso do check-in. Ver RegraXpService para de onde vem
   cada linha.

   Leitura liberada para qualquer autenticado — é o card "COMO GANHAR XP"
   da área /participantes. Escrita restrita a DIRETOR_SITE/PRESIDENTE
   (ver SecurityConfig), os mesmos que editam níveis. */
@RestController
@RequestMapping("/api/regra-xp")
public class RegraXpController {

    private final RegraXpService regraXpService;

    public RegraXpController(RegraXpService regraXpService) {
        this.regraXpService = regraXpService;
    }

    @GetMapping
    public List<RegraXpResponseDTO> listar() {
        return regraXpService.listar();
    }

    @PutMapping("/{chave}")
    public RegraXpResponseDTO atualizar(@PathVariable String chave,
                                        @Valid @RequestBody RegraXpRequestDTO dto) {
        return regraXpService.atualizar(chave, dto);
    }
}
