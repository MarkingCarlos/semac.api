package com.semac.java_api.controller;

import com.semac.java_api.dto.LaboratorioAdminDTO;
import com.semac.java_api.dto.LaboratorioRequestDTO;
import com.semac.java_api.dto.QuizAbertoRequestDTO;
import com.semac.java_api.dto.QuizLaboratorioDTO;
import com.semac.java_api.dto.RespostaQuizLaboratorioRequestDTO;
import com.semac.java_api.dto.ResultadoQuizLaboratorioDTO;
import com.semac.java_api.service.LaboratorioQuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/* Quiz dos Laboratórios: responder (/laboratorio/<codigo>) e o cadastro
   em /admin -> Informações SEMAC -> Gamificação.

   Acessos (ver SecurityConfig): abrir e responder o quiz é do
   participante, e a pessoa vem sempre da claim `id` do token; cadastrar
   laboratórios é de PAPEIS_GAMIFICACAO, mesmo público da Mostra. */
@RestController
@RequestMapping("/api/laboratorio")
public class LaboratorioQuizController {

    private final LaboratorioQuizService laboratorioQuizService;

    public LaboratorioQuizController(LaboratorioQuizService laboratorioQuizService) {
        this.laboratorioQuizService = laboratorioQuizService;
    }

    /* ── Participante ────────────────────────────────────────────── */

    @GetMapping("/quiz/{codigo}")
    public QuizLaboratorioDTO abrirQuiz(@PathVariable String codigo, @AuthenticationPrincipal Jwt jwt) {
        return laboratorioQuizService.abrirQuiz(idDoToken(jwt), codigo);
    }

    @PostMapping("/quiz/{codigo}/resposta")
    public ResultadoQuizLaboratorioDTO responder(@PathVariable String codigo,
                                                 @Valid @RequestBody RespostaQuizLaboratorioRequestDTO dto,
                                                 @AuthenticationPrincipal Jwt jwt) {
        return laboratorioQuizService.responder(idDoToken(jwt), codigo, dto);
    }

    /* ── Comissão ────────────────────────────────────────────────── */

    @GetMapping("/laboratorios")
    public List<LaboratorioAdminDTO> listar(@RequestParam Integer ano) {
        return laboratorioQuizService.listarParaAdmin(ano);
    }

    @PostMapping("/laboratorios")
    @ResponseStatus(HttpStatus.CREATED)
    public LaboratorioAdminDTO criar(@Valid @RequestBody LaboratorioRequestDTO dto) {
        return laboratorioQuizService.criar(dto);
    }

    @PutMapping("/laboratorios/{id}")
    public LaboratorioAdminDTO atualizar(@PathVariable Integer id, @Valid @RequestBody LaboratorioRequestDTO dto) {
        return laboratorioQuizService.atualizar(id, dto);
    }

    @PatchMapping("/laboratorios/{id}/quiz-aberto")
    public LaboratorioAdminDTO definirQuizAberto(@PathVariable Integer id, @Valid @RequestBody QuizAbertoRequestDTO dto) {
        return laboratorioQuizService.definirQuizAberto(id, dto.aberto());
    }

    /* Modo teste: a comissão responde antes de liberar, sem gravar
       tentativa nem creditar xp (ver LaboratorioQuizService.abrirTeste). */
    @GetMapping("/laboratorios/{id}/teste")
    public QuizLaboratorioDTO abrirTeste(@PathVariable Integer id) {
        return laboratorioQuizService.abrirTeste(id);
    }

    @PostMapping("/laboratorios/{id}/teste/resposta")
    public ResultadoQuizLaboratorioDTO responderTeste(@PathVariable Integer id,
                                                      @Valid @RequestBody RespostaQuizLaboratorioRequestDTO dto) {
        return laboratorioQuizService.responderTeste(id, dto);
    }

    @DeleteMapping("/laboratorios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Integer id) {
        laboratorioQuizService.remover(id);
    }

    private Integer idDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (id instanceof Number numero) {
            return numero.intValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
    }
}
