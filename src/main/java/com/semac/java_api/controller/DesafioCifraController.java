package com.semac.java_api.controller;

import com.semac.java_api.dto.DesafioCifraAdminDTO;
import com.semac.java_api.dto.DesafioCifraDiaDTO;
import com.semac.java_api.dto.DesafioCifraEstadoDTO;
import com.semac.java_api.dto.DesafioCifraTentativaDTO;
import com.semac.java_api.dto.DesafioCifraTentativaRespostaDTO;
import com.semac.java_api.dto.DesafioCifraTesteRespostaDTO;
import com.semac.java_api.dto.DesafioCifraTesteTentativaDTO;
import com.semac.java_api.service.DesafioCifraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/* Desafio diário "Cifra do Dia" (/desafio-cifra) e o cadastro dos dias em
   /admin -> Cifra do Dia.

   Nenhuma rota do participante devolve a resposta da etapa aberta — nem
   ao perder. A única que traz respostas é a listagem da diretoria.

   Não há rota de cadastro: os desafios entram por migration, e o admin
   só visualiza e testa (ver DesafioCifraService.listarParaAdmin).

   Acessos (ver SecurityConfig): jogar é do participante, porque o acerto
   credita xp e as tentativas são contadas por pessoa; ver e testar é de
   PAPEIS_GAMIFICACAO, mesmo público do Termo. */
@RestController
@RequestMapping("/api/desafio-cifra")
public class DesafioCifraController {

    private final DesafioCifraService desafioCifraService;

    public DesafioCifraController(DesafioCifraService desafioCifraService) {
        this.desafioCifraService = desafioCifraService;
    }

    /* ── Jogador ─────────────────────────────────────────────────── */

    @GetMapping("/hoje")
    public DesafioCifraEstadoDTO hoje(@AuthenticationPrincipal Jwt jwt) {
        return desafioCifraService.estadoDeHoje(idDoToken(jwt));
    }

    @GetMapping("/meus")
    public List<DesafioCifraDiaDTO> meus(@AuthenticationPrincipal Jwt jwt) {
        return desafioCifraService.listarDiasDoParticipante(idDoToken(jwt));
    }

    @PostMapping("/tentativa")
    public DesafioCifraTentativaRespostaDTO tentar(@Valid @RequestBody DesafioCifraTentativaDTO dto,
                                                   @AuthenticationPrincipal Jwt jwt) {
        return desafioCifraService.tentar(idDoToken(jwt), dto.resposta());
    }

    /* ── Diretoria ───────────────────────────────────────────────── */

    @GetMapping("/admin")
    public List<DesafioCifraAdminDTO> listar(@RequestParam Integer ano) {
        return desafioCifraService.listarParaAdmin(ano);
    }

    /* Modo teste: a diretoria joga um desafio cadastrado sem gravar jogo
       nem creditar xp (ver DesafioCifraService.abrirTeste). */
    @GetMapping("/teste/{id}")
    public DesafioCifraEstadoDTO abrirTeste(@PathVariable Integer id) {
        return desafioCifraService.abrirTeste(id);
    }

    @PostMapping("/teste/tentativa")
    public DesafioCifraTesteRespostaDTO tentarTeste(@Valid @RequestBody DesafioCifraTesteTentativaDTO dto) {
        return desafioCifraService.tentarTeste(dto);
    }

    private Integer idDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (id instanceof Number numero) {
            return numero.intValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
    }
}
