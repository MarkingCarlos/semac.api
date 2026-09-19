package com.semac.java_api.controller;

import com.semac.java_api.dto.ModeloEmailRequestDTO;
import com.semac.java_api.dto.ModeloEmailResponseDTO;
import com.semac.java_api.dto.PreviaEmailRequestDTO;
import com.semac.java_api.dto.PreviaEmailResponseDTO;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.service.EmailService;
import com.semac.java_api.service.ModeloEmailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/* Textos das mensagens automáticas, editados na aba "Mensagens" do /admin.
   Restrito a DIRETOR_SITE e PRESIDENTE no SecurityConfig: mudar o texto de
   um e-mail que sai para todos os inscritos não é ação de rotina. */
@RestController
@RequestMapping("/api/admin/modelos-email")
public class ModeloEmailController {

    private final ModeloEmailService modeloEmailService;
    private final EmailService emailService;
    private final PessoaRepository pessoaRepository;

    public ModeloEmailController(ModeloEmailService modeloEmailService,
                                 EmailService emailService,
                                 PessoaRepository pessoaRepository) {
        this.modeloEmailService = modeloEmailService;
        this.emailService = emailService;
        this.pessoaRepository = pessoaRepository;
    }

    @GetMapping
    public List<ModeloEmailResponseDTO> listar() {
        return modeloEmailService.listar();
    }

    @GetMapping("/{chave}")
    public ModeloEmailResponseDTO buscar(@PathVariable String chave) {
        return modeloEmailService.buscar(chave);
    }

    @PutMapping("/{chave}")
    public ModeloEmailResponseDTO atualizar(@PathVariable String chave,
                                            @Valid @RequestBody ModeloEmailRequestDTO dto,
                                            @AuthenticationPrincipal Jwt jwt) {
        return modeloEmailService.atualizar(chave, dto, idDoToken(jwt));
    }

    /* Renderiza o que está na tela, sem salvar — é o que alimenta o iframe
       de prévia enquanto a pessoa digita. */
    @PostMapping("/{chave}/previa")
    public PreviaEmailResponseDTO previa(@PathVariable String chave,
                                         @Valid @RequestBody PreviaEmailRequestDTO dto) {
        return modeloEmailService.previa(chave, dto.assunto(), dto.corpoMarkdown());
    }

    /* Manda a prévia para o e-mail de quem está logado. O iframe mostra o
       HTML; só o envio real revela como o Gmail de fato renderiza. */
    @PostMapping("/{chave}/teste")
    public ResponseEntity<Void> enviarTeste(@PathVariable String chave,
                                            @Valid @RequestBody PreviaEmailRequestDTO dto,
                                            @AuthenticationPrincipal Jwt jwt) {
        Pessoa editor = pessoaRepository.findById(idDoToken(jwt))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));

        PreviaEmailResponseDTO previa = modeloEmailService.previa(chave, dto.assunto(), dto.corpoMarkdown());
        emailService.enviarHtmlPronto(editor.getEmail(), "[TESTE] " + previa.assunto(), previa.html());

        return ResponseEntity.accepted().build();
    }

    /* Extrai o id da pessoa da claim `id` do token (gravada no login). */
    private Integer idDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (id instanceof Number numero) {
            return numero.intValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
    }
}
