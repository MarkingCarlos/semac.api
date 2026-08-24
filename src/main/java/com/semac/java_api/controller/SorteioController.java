package com.semac.java_api.controller;

import com.semac.java_api.dto.ParticipanteElegivelDTO;
import com.semac.java_api.dto.SorteioRequestDTO;
import com.semac.java_api.dto.SorteioResponseDTO;
import com.semac.java_api.service.SorteioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/* Sorteio de brindes, usado pela tela /sorteio. O organizador é sempre
   identificado pela claim `id` do Bearer token — nunca por parâmetro
   (mesmo padrão de InscricaoEventoController), já que só quem tem sessão
   de comissão consegue realizar um sorteio. */
@RestController
@RequestMapping("/api/sorteio")
public class SorteioController {

    private final SorteioService sorteioService;

    public SorteioController(SorteioService sorteioService) {
        this.sorteioService = sorteioService;
    }

    @GetMapping("/elegiveis")
    public List<ParticipanteElegivelDTO> elegiveis(@RequestParam Integer eventoId) {
        return sorteioService.elegiveis(eventoId);
    }

    @PostMapping
    public ResponseEntity<SorteioResponseDTO> registrar(@AuthenticationPrincipal Jwt jwt,
                                                         @Valid @RequestBody SorteioRequestDTO dto) {
        SorteioResponseDTO resposta = sorteioService.registrarGanhador(
                dto.eventoId(), dto.brindeId(), dto.participanteId(), idDoToken(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    private Integer idDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (id instanceof Number numero) {
            return numero.intValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
    }
}
