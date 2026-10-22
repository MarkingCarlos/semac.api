package com.semac.java_api.controller;

import com.semac.java_api.dto.DebatePerguntasAbertasDTO;
import com.semac.java_api.dto.PerguntaDebateEnviadaDTO;
import com.semac.java_api.dto.PerguntaRequestDTO;
import com.semac.java_api.service.PerguntaDebateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/* Envio público de perguntas para os debates — página /perguntas e o
   botão "Mande sua pergunta" do /participantes. Aberto a qualquer pessoa
   (ver SecurityConfig): com token, a pergunta fica ligada à conta (claim
   `id`, nunca por parâmetro) e o participante pode ganhar a conquista; sem
   token, vale o nome digitado, se houver. A leitura segue no
   PerguntaDebateController, restrita à diretoria. */
@RestController
@RequestMapping("/api/perguntas")
public class PerguntaController {

    private final PerguntaDebateService perguntaDebateService;

    public PerguntaController(PerguntaDebateService perguntaDebateService) {
        this.perguntaDebateService = perguntaDebateService;
    }

    @GetMapping("/debates")
    public List<DebatePerguntasAbertasDTO> listarDebatesAbertos() {
        return perguntaDebateService.listarDebatesAbertos();
    }

    @PostMapping
    public ResponseEntity<PerguntaDebateEnviadaDTO> enviar(@AuthenticationPrincipal Jwt jwt,
                                                           @Valid @RequestBody PerguntaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(perguntaDebateService.enviar(idDoTokenOuNulo(jwt), dto.eventoId(), dto.texto(), dto.nome()));
    }

    /* Sem token (visitante, professor) o jwt chega null. */
    private Integer idDoTokenOuNulo(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        return id instanceof Number numero ? numero.intValue() : null;
    }
}
