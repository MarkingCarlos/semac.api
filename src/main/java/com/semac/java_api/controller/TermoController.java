package com.semac.java_api.controller;

import com.semac.java_api.dto.TermoDiaAdminDTO;
import com.semac.java_api.dto.TermoDiaDTO;
import com.semac.java_api.dto.TermoDiaRequestDTO;
import com.semac.java_api.dto.TermoEstadoDTO;
import com.semac.java_api.dto.TermoPalavraRequestDTO;
import com.semac.java_api.dto.TermoPalavraValidaDTO;
import com.semac.java_api.dto.TermoPalpiteDTO;
import com.semac.java_api.dto.TermoPalpiteRespostaDTO;
import com.semac.java_api.dto.TermoPalpiteTesteDTO;
import com.semac.java_api.dto.TermoPalpiteTesteRespostaDTO;
import com.semac.java_api.dto.TermoTesteDTO;
import com.semac.java_api.service.TermoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/* Jogo do Termo (/termo) e o cadastro dos dias e palavras em /admin -> Termo.

   Nenhuma rota do participante carrega a palavra secreta enquanto o jogo
   está em andamento. O palpite é conferido no servidor e volta só o padrão
   de cores. A única que devolve as palavras é a listagem da diretoria
   (GET /dias, ver TermoPalavraAdminDTO).

   Cada dia tem várias palavras, e cada participante recebe uma por
   sorteio (ver TermoService.sortearJogo).

   Acessos (ver SecurityConfig): jogar é do participante, porque a vitória
   credita xp e as tentativas são contadas por pessoa; cadastrar dias e
   palavras é de DIRETOR_SITE/PRESIDENTE, mesmo público das cotas e dos
   níveis. */
@RestController
@RequestMapping("/api/termo")
public class TermoController {

    private final TermoService termoService;

    public TermoController(TermoService termoService) {
        this.termoService = termoService;
    }

    /* ── Jogador ─────────────────────────────────────────────────── */

    /* Estado do jogo de hoje, já retomando a partida em andamento. O
       usuário vem da claim `id` do token — nunca por parâmetro, mesmo
       critério de GET /api/conquista/minhas. */
    @GetMapping("/hoje")
    public TermoEstadoDTO hoje(@AuthenticationPrincipal Jwt jwt) {
        return termoService.estadoDeHoje(idDoToken(jwt));
    }

    /* Os dias de Termo que já aconteceram e o que o participante fez em
       cada um — o histórico da aba "Desafios". Mesma regra de `/hoje`: a
       pessoa vem do token, nunca por parâmetro. */
    @GetMapping("/meus")
    public List<TermoDiaDTO> meus(@AuthenticationPrincipal Jwt jwt) {
        return termoService.listarDiasDoParticipante(idDoToken(jwt));
    }

    @PostMapping("/palpite")
    public TermoPalpiteRespostaDTO palpitar(@Valid @RequestBody TermoPalpiteDTO dto,
                                            @AuthenticationPrincipal Jwt jwt) {
        return termoService.palpitar(idDoToken(jwt), dto.palpite());
    }

    /* Se a palavra está no dicionário — para o modo treino, que confere o
       palpite no navegador mas não carrega a lista. */
    @GetMapping("/validar")
    public TermoPalavraValidaDTO validar(@RequestParam String palavra) {
        return termoService.validarPalavra(palavra);
    }

    /* ── Diretoria ───────────────────────────────────────────────── */

    @GetMapping("/dias")
    public List<TermoDiaAdminDTO> listarDias(@RequestParam Integer ano) {
        return termoService.listarParaAdmin(ano);
    }

    @PutMapping("/dias/{dia}")
    public TermoDiaAdminDTO salvarDia(@PathVariable Integer dia,
                                      @Valid @RequestBody TermoDiaRequestDTO dto) {
        return termoService.salvarDia(dia, dto);
    }

    @PostMapping("/dias/{dia}/palavras")
    public TermoDiaAdminDTO adicionarPalavras(@PathVariable Integer dia,
                                              @Valid @RequestBody TermoPalavraRequestDTO dto) {
        return termoService.adicionarPalavras(dia, dto);
    }

    @DeleteMapping("/palavras/{id}")
    public TermoDiaAdminDTO removerPalavra(@PathVariable Integer id) {
        return termoService.removerPalavra(id);
    }

    /* Modo teste: a diretoria joga contra uma palavra cadastrada, sem
       gravar jogo nem creditar xp (ver TermoService.palpitarTeste). */
    @GetMapping("/teste/{palavraId}")
    public TermoTesteDTO abrirTeste(@PathVariable Integer palavraId) {
        return termoService.abrirTeste(palavraId);
    }

    @PostMapping("/teste/palpite")
    public TermoPalpiteTesteRespostaDTO palpitarTeste(@Valid @RequestBody TermoPalpiteTesteDTO dto) {
        return termoService.palpitarTeste(dto.palavraId(), dto.palpite());
    }

    private Integer idDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (id instanceof Number numero) {
            return numero.intValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
    }
}
