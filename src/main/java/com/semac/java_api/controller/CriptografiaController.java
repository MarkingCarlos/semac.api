package com.semac.java_api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.semac.java_api.dto.CriptografiaEstadoDTO;
import com.semac.java_api.dto.CriptografiaPalpiteDTO;
import com.semac.java_api.dto.CriptografiaPalpiteRespostaDTO;
import com.semac.java_api.service.CriptografiaService;

@RestController
@RequestMapping("/api/criptografia")
public class CriptografiaController {
    
    private final CriptografiaService criptografiaService;

    public CriptografiaController(CriptografiaService criptografiaService){
        this.criptografiaService = criptografiaService;
    }

    /* Ler o estado atual do jogo da Criptografia. */
    @GetMapping("/estado")
    public CriptografiaEstadoDTO lerEstado(@AuthenticationPrincipal Jwt jwt) {
        return criptografiaService.estadoDeHoje(idDoToken(jwt));
    }

    /* Enviar um palpite para o jogo da Criptografia. */
    @PostMapping("/palpite")
    public CriptografiaPalpiteRespostaDTO palpitar(@Valid @RequestBody              CriptografiaPalpiteDTO dto,
                                            @AuthenticationPrincipal Jwt jwt) {
        return criptografiaService.palpitar(idDoToken(jwt), dto.palpite());
    }

    private Integer idDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (id instanceof Number numero) {
            return numero.intValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
    }
}
