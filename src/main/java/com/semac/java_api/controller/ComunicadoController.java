package com.semac.java_api.controller;

import com.semac.java_api.dto.*;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.enums.PublicoComunicado;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.service.ComunicadoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/* Comunicados avulsos, disparados na aba "Comunicados" do /admin.
   Restrito a DIRETOR_SITE e PRESIDENTE no SecurityConfig: mandar e-mail
   para todos os inscritos é irreversível e consome a cota diária do
   Gmail. */
@RestController
@RequestMapping("/api/comunicados")
public class ComunicadoController {

    private final ComunicadoService comunicadoService;
    private final PessoaRepository pessoaRepository;

    public ComunicadoController(ComunicadoService comunicadoService, PessoaRepository pessoaRepository) {
        this.comunicadoService = comunicadoService;
        this.pessoaRepository = pessoaRepository;
    }

    /* Públicos disponíveis, já com a contagem de cada um. */
    @GetMapping("/publicos")
    public List<PublicoComunicadoDTO> publicos() {
        return comunicadoService.publicos();
    }

    /* Quantos vão receber, mais alguns nomes: é o que a tela mostra na
       confirmação, antes de disparar. */
    @GetMapping("/destinatarios")
    public DestinatariosResponseDTO destinatarios(@RequestParam String publico,
                                                  @RequestParam(required = false) Integer eventoId) {
        return comunicadoService.destinatarios(PublicoComunicado.deTexto(publico), eventoId);
    }

    @PostMapping("/previa")
    public PreviaEmailResponseDTO previa(@Valid @RequestBody PreviaEmailRequestDTO dto) {
        return new PreviaEmailResponseDTO(dto.assunto(), comunicadoService.previa(dto.corpoMarkdown()));
    }

    @PostMapping("/teste")
    public ResponseEntity<Void> enviarTeste(@Valid @RequestBody PreviaEmailRequestDTO dto,
                                            @AuthenticationPrincipal Jwt jwt) {
        comunicadoService.enviarTeste(autorDoToken(jwt), dto.assunto(), dto.corpoMarkdown());
        return ResponseEntity.accepted().build();
    }

    /* 202, não 200: o lote leva minutos e segue em segundo plano. O
       histórico é quem conta como terminou. */
    @PostMapping
    public ResponseEntity<ComunicadoResponseDTO> disparar(@Valid @RequestBody ComunicadoRequestDTO dto,
                                                          @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(comunicadoService.disparar(dto, autorDoToken(jwt).getId()));
    }

    @GetMapping
    public List<ComunicadoResponseDTO> historico() {
        return comunicadoService.historico();
    }

    private Pessoa autorDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (!(id instanceof Number numero)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
        }
        return pessoaRepository.findById(numero.intValue())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    }
}
