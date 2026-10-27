package com.semac.java_api.controller;

import com.semac.java_api.dto.AvaliacaoEventoEnviadaDTO;
import com.semac.java_api.dto.AvaliacaoEventoRequestDTO;
import com.semac.java_api.dto.MinhaAvaliacaoEventoDTO;
import com.semac.java_api.dto.ResumoAvaliacaoEventoDTO;
import com.semac.java_api.service.AvaliacaoEventoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/* Pesquisa de satisfação dos eventos (ver AvaliacaoEventoService). O
   participante é sempre identificado pela claim `id` do Bearer token —
   nunca por parâmetro, para ninguém avaliar no lugar de outra pessoa.
   Quem acessa cada rota é decidido no SecurityConfig. */
@RestController
@RequestMapping("/api/evento")
public class AvaliacaoEventoController {

    private final AvaliacaoEventoService avaliacaoEventoService;

    public AvaliacaoEventoController(AvaliacaoEventoService avaliacaoEventoService) {
        this.avaliacaoEventoService = avaliacaoEventoService;
    }

    /* Cria ou troca a nota do participante logado. Recusas de regra (tipo
       fora da pesquisa, evento não terminado, sem presença) voltam 409. */
    @PutMapping("/{id}/avaliacao")
    public AvaliacaoEventoEnviadaDTO avaliar(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable Integer id,
                                             @Valid @RequestBody AvaliacaoEventoRequestDTO dto) {
        return avaliacaoEventoService.avaliar(idDoToken(jwt), id, dto.nota());
    }

    /* Notas que o participante logado já deu. */
    @GetMapping("/avaliacoes/minhas")
    public List<MinhaAvaliacaoEventoDTO> minhasAvaliacoes(@AuthenticationPrincipal Jwt jwt) {
        return avaliacaoEventoService.listarDoParticipante(idDoToken(jwt));
    }

    /* Resumo do /admin: média e quantidade de notas por evento. */
    @GetMapping("/avaliacoes")
    public List<ResumoAvaliacaoEventoDTO> resumo() {
        return avaliacaoEventoService.resumir();
    }

    private Integer idDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (id instanceof Number numero) {
            return numero.intValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
    }
}
