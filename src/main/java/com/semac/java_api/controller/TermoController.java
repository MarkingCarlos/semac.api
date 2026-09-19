package com.semac.java_api.controller;

import com.semac.java_api.dto.TermoDiaDTO;
import com.semac.java_api.dto.TermoEstadoDTO;
import com.semac.java_api.dto.TermoPalavraAdminDTO;
import com.semac.java_api.dto.TermoPalavraRequestDTO;
import com.semac.java_api.dto.TermoPalpiteDTO;
import com.semac.java_api.dto.TermoPalpiteRespostaDTO;
import com.semac.java_api.service.TermoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/* Jogo do Termo (/termo) e o cadastro das palavras em /admin -> Termo.

   Nenhuma resposta daqui carrega a palavra secreta enquanto o jogo está em
   andamento — nem para o participante que está jogando, nem para a
   diretoria que a cadastrou (ver TermoPalavraAdminDTO). O palpite é
   conferido no servidor e volta só o padrão de cores.

   Acessos (ver SecurityConfig): jogar é do participante, porque a vitória
   credita xp e as tentativas são contadas por pessoa; cadastrar a palavra
   é de DIRETOR_SITE/PRESIDENTE, mesmo público das cotas e dos níveis. */
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

    /* ── Diretoria ───────────────────────────────────────────────── */

    @GetMapping("/palavras")
    public List<TermoPalavraAdminDTO> listarPalavras(@RequestParam Integer ano) {
        return termoService.listarParaAdmin(ano);
    }

    @PutMapping("/palavras/{dia}")
    public TermoPalavraAdminDTO salvarPalavra(@PathVariable Integer dia,
                                              @Valid @RequestBody TermoPalavraRequestDTO dto) {
        return termoService.salvar(dia, dto);
    }

    private Integer idDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (id instanceof Number numero) {
            return numero.intValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
    }
}
