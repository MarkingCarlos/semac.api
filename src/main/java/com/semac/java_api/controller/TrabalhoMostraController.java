package com.semac.java_api.controller;

import com.semac.java_api.dto.MostraProgressoDTO;
import com.semac.java_api.dto.TrabalhoMostraAdminDTO;
import com.semac.java_api.dto.TrabalhoMostraRequestDTO;
import com.semac.java_api.dto.VisitaMostraRespostaDTO;
import com.semac.java_api.service.TrabalhoMostraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/* Mostra Técnica: a visita aos stands (/mostra/<codigo>) e o cadastro dos
   trabalhos em /admin -> Informações SEMAC -> Gamificação.

   Acessos (ver SecurityConfig): visitar e ver o progresso é do
   participante, e a pessoa vem sempre da claim `id` do token; cadastrar
   trabalhos é de PAPEIS_GAMIFICACAO, mesmo público do Termo. */
@RestController
@RequestMapping("/api/mostra")
public class TrabalhoMostraController {

    private final TrabalhoMostraService trabalhoMostraService;

    public TrabalhoMostraController(TrabalhoMostraService trabalhoMostraService) {
        this.trabalhoMostraService = trabalhoMostraService;
    }

    /* ── Participante ────────────────────────────────────────────── */

    @PostMapping("/visitar/{codigo}")
    public VisitaMostraRespostaDTO visitar(@PathVariable String codigo, @AuthenticationPrincipal Jwt jwt) {
        return trabalhoMostraService.visitar(idDoToken(jwt), codigo);
    }

    @GetMapping("/minhas")
    public MostraProgressoDTO minhas(@AuthenticationPrincipal Jwt jwt) {
        return trabalhoMostraService.progresso(idDoToken(jwt));
    }

    /* ── Diretoria ───────────────────────────────────────────────── */

    @GetMapping("/trabalhos")
    public List<TrabalhoMostraAdminDTO> listar(@RequestParam Integer ano) {
        return trabalhoMostraService.listarParaAdmin(ano);
    }

    @PostMapping("/trabalhos")
    @ResponseStatus(HttpStatus.CREATED)
    public TrabalhoMostraAdminDTO criar(@Valid @RequestBody TrabalhoMostraRequestDTO dto) {
        return trabalhoMostraService.criar(dto);
    }

    @PutMapping("/trabalhos/{id}")
    public TrabalhoMostraAdminDTO atualizar(@PathVariable Integer id,
                                            @Valid @RequestBody TrabalhoMostraRequestDTO dto) {
        return trabalhoMostraService.atualizar(id, dto);
    }

    @DeleteMapping("/trabalhos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Integer id) {
        trabalhoMostraService.remover(id);
    }

    private Integer idDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (id instanceof Number numero) {
            return numero.intValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
    }
}
