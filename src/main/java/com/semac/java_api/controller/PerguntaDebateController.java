package com.semac.java_api.controller;

import com.semac.java_api.dto.PerguntaAdminRequestDTO;
import com.semac.java_api.dto.PerguntaDebateResponseDTO;
import com.semac.java_api.service.PerguntaDebateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/* Leitura das perguntas de um debate e inclusão à mão pelo /admin —
   diretoria e presidência, ver SecurityConfig. O envio público fica no
   PerguntaController (/api/perguntas). */
@RestController
@RequestMapping("/api/evento/{id}/perguntas")
public class PerguntaDebateController {

    private final PerguntaDebateService perguntaDebateService;

    public PerguntaDebateController(PerguntaDebateService perguntaDebateService) {
        this.perguntaDebateService = perguntaDebateService;
    }

    @GetMapping
    public List<PerguntaDebateResponseDTO> listar(@PathVariable Integer id) {
        return perguntaDebateService.listar(id);
    }

    /* Quem cadastrou sai do token (claim `id`), nunca do corpo. */
    @PostMapping
    public ResponseEntity<PerguntaDebateResponseDTO> adicionar(@AuthenticationPrincipal Jwt jwt,
                                                               @PathVariable Integer id,
                                                               @Valid @RequestBody PerguntaAdminRequestDTO dto) {
        Object adminId = jwt == null ? null : jwt.getClaim("id");
        return ResponseEntity.status(HttpStatus.CREATED).body(perguntaDebateService.adicionarPelaComissao(
                adminId instanceof Number numero ? numero.intValue() : null, id, dto.texto(), dto.nomeAutor(),
                dto.bloco(), Boolean.TRUE.equals(dto.daMediacao())));
    }
}
