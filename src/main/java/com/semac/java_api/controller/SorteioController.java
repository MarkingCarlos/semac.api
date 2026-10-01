package com.semac.java_api.controller;

import com.semac.java_api.dto.EntregaSorteioRequestDTO;
import com.semac.java_api.dto.GanhadorSorteioDTO;
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

/* Sorteios de brindes. O CRUD (nome + evento) é gerenciado no /admin,
   aba Brindes → Sorteios; a realização (elegíveis + entrega) é usada
   pela tela /sorteio. O organizador da entrega é sempre identificado
   pela claim `id` do Bearer token — nunca por parâmetro (mesmo padrão
   de InscricaoEventoController). */
@RestController
@RequestMapping("/api/sorteio")
public class SorteioController {

    private final SorteioService sorteioService;

    public SorteioController(SorteioService sorteioService) {
        this.sorteioService = sorteioService;
    }

    @GetMapping
    public List<SorteioResponseDTO> listar() {
        return sorteioService.listar();
    }

    @PostMapping
    public ResponseEntity<SorteioResponseDTO> criar(@Valid @RequestBody SorteioRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sorteioService.criar(dto));
    }

    @PutMapping("/{id}")
    public SorteioResponseDTO atualizar(@PathVariable Integer id, @Valid @RequestBody SorteioRequestDTO dto) {
        return sorteioService.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        sorteioService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/elegiveis")
    public List<ParticipanteElegivelDTO> elegiveis(@PathVariable Integer id) {
        return sorteioService.elegiveis(id);
    }

    @PostMapping("/{id}/entrega")
    public ResponseEntity<GanhadorSorteioDTO> registrarEntrega(@AuthenticationPrincipal Jwt jwt,
                                                               @PathVariable Integer id,
                                                               @Valid @RequestBody EntregaSorteioRequestDTO dto) {
        GanhadorSorteioDTO resposta = sorteioService.registrarGanhador(
                id, dto.brindeId(), dto.participanteId(), idDoToken(jwt));
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
