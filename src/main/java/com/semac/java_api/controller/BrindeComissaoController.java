package com.semac.java_api.controller;

import com.semac.java_api.dto.BrindeComissaoEntregueDTO;
import com.semac.java_api.dto.MeuBrindeComissaoDTO;
import com.semac.java_api.dto.RetiradaBrindeComissaoDTO;
import com.semac.java_api.service.BrindeComissaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/* Brinde (Monster) da comissão e dos participantes (26/10 e 29/10). As
   duas rotas são de qualquer papel de comissão (ver SecurityConfig): cada
   um vê o próprio QR e qualquer um entrega — inclusive lendo o crachá de
   participante. Os ids vêm sempre do token. */
@RestController
@RequestMapping("/api/brinde-comissao")
public class BrindeComissaoController {

    private final BrindeComissaoService brindeComissaoService;

    public BrindeComissaoController(BrindeComissaoService brindeComissaoService) {
        this.brindeComissaoService = brindeComissaoService;
    }

    @GetMapping("/meu")
    public MeuBrindeComissaoDTO meu(@AuthenticationPrincipal Jwt jwt) {
        return brindeComissaoService.meuBrinde(idDoToken(jwt));
    }

    @PostMapping("/retirada")
    public BrindeComissaoEntregueDTO retirar(@Valid @RequestBody RetiradaBrindeComissaoDTO dto,
                                             @AuthenticationPrincipal Jwt jwt) {
        return brindeComissaoService.retirar(dto.qrCode(), idDoToken(jwt));
    }

    private Integer idDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (id instanceof Number numero) {
            return numero.intValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
    }
}
