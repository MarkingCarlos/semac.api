package com.semac.java_api.controller;

import com.semac.java_api.dto.AtivoRequestDTO;
import com.semac.java_api.dto.AtribuirRoleDTO;
import com.semac.java_api.dto.AtualizarPerfilDTO;
import com.semac.java_api.dto.InscricaoFinanceiraDTO;
import com.semac.java_api.dto.ParticipanteResponseDTO;
import com.semac.java_api.dto.PerfilResponseDTO;
import com.semac.java_api.service.PessoaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/pessoa")
public class PessoaController {

    private final PessoaService pessoaService;

    public PessoaController(PessoaService pessoaService) {
        this.pessoaService = pessoaService;
    }

    /* Lista para a tabela de participantes do /admin (confirmados + pendentes). */
    @GetMapping("/participantes")
    public List<ParticipanteResponseDTO> listarParticipantes() {
        return pessoaService.listarParticipantes();
    }

    /* Lista a comissão organizadora do /admin (papéis != PARTICIPANTE). */
    @GetMapping("/comissao")
    public List<ParticipanteResponseDTO> listarComissao() {
        return pessoaService.listarComissao();
    }

    /* Inscrições confirmadas para o módulo financeiro (somente leitura). */
    @GetMapping("/inscricoes")
    public List<InscricaoFinanceiraDTO> listarInscricoes() {
        return pessoaService.listarInscricoes();
    }

    /* Perfil do próprio usuário logado (seção Início do /admin). O usuário é
       identificado pela claim `id` do Bearer token — nunca por parâmetro. */
    @GetMapping("/me")
    public PerfilResponseDTO meuPerfil(@AuthenticationPrincipal Jwt jwt) {
        return pessoaService.buscarPerfil(idDoToken(jwt));
    }

    /* Atualiza os campos editáveis do próprio perfil (RA e camiseta). */
    @PatchMapping("/me")
    public PerfilResponseDTO atualizarMeuPerfil(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody AtualizarPerfilDTO dto) {
        return pessoaService.atualizarPerfil(idDoToken(jwt), dto);
    }

    /* Extrai o id da pessoa da claim `id` do token (gravada no login). */
    private Integer idDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (id instanceof Number numero) {
            return numero.intValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
    }

    /* Confirma a inscrição atribuindo o papel (PARTICIPANTE ou comissão). */
    @PatchMapping("/{id}/role")
    public ParticipanteResponseDTO atribuirRole(@PathVariable Integer id,
                                                @Valid @RequestBody AtribuirRoleDTO dto) {
        return pessoaService.atribuirRole(id, dto.role(), dto.tipoInscricaoId());
    }

    /* Ativa/desativa uma pessoa (ex.: suspender membro da comissão). */
    @PatchMapping("/{id}/ativo")
    public ParticipanteResponseDTO definirAtivo(@PathVariable Integer id,
                                                @Valid @RequestBody AtivoRequestDTO dto) {
        return pessoaService.definirAtivo(id, dto.ativo());
    }
}
