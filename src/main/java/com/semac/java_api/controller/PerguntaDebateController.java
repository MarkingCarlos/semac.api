package com.semac.java_api.controller;

import com.semac.java_api.dto.PerguntaDebateResponseDTO;
import com.semac.java_api.service.PerguntaDebateService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/* Leitura das perguntas de um debate — diretoria e presidência, ver
   SecurityConfig. O envio fica no PerguntaController (/api/perguntas),
   público. */
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
}
