package com.semac.java_api.controller;

import com.semac.java_api.dto.PerguntaDebateRequestDTO;
import com.semac.java_api.dto.PerguntaDebateResponseDTO;
import com.semac.java_api.service.PerguntaDebateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/* Perguntas para os debates. O POST é do participante (identificado pela
   claim `id` do token, nunca por parâmetro); o GET é da diretoria e da
   presidência — ver SecurityConfig. */
@RestController
@RequestMapping("/api/evento/{id}/perguntas")
public class PerguntaDebateController {

    private final PerguntaDebateService perguntaDebateService;

    public PerguntaDebateController(PerguntaDebateService perguntaDebateService) {
        this.perguntaDebateService = perguntaDebateService;
    }

    @PostMapping
    public ResponseEntity<Void> enviar(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable Integer id,
                                       @Valid @RequestBody PerguntaDebateRequestDTO dto) {
        perguntaDebateService.enviar(idDoToken(jwt), id, dto.texto());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public List<PerguntaDebateResponseDTO> listar(@PathVariable Integer id) {
        return perguntaDebateService.listar(id);
    }

    private Integer idDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (id instanceof Number numero) {
            return numero.intValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
    }
}
