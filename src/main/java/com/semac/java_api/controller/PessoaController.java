package com.semac.java_api.controller;

import com.semac.java_api.dto.AtivoRequestDTO;
import com.semac.java_api.dto.AtribuirRoleDTO;
import com.semac.java_api.dto.AtualizarCamisetasRequestDTO;
import com.semac.java_api.dto.AtualizarPerfilDTO;
import com.semac.java_api.dto.InscricaoFinanceiraDTO;
import com.semac.java_api.dto.ParticipanteResponseDTO;
import com.semac.java_api.dto.PerfilResponseDTO;
import com.semac.java_api.service.PessoaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/pessoa")
public class PessoaController {

    private final PessoaService pessoaService;

    @Value("${app.upload.dir}")
    private String uploadDir;

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

    /* Exclui definitivamente um participante ou membro da comissão. */
    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Integer id) {
        pessoaService.excluir(id);
    }

    /* Substitui a lista de camisetas da pessoa (replace-all) — editor do
       /admin restrito a DIRETOR_SITE/PRESIDENTE (ver SecurityConfig). */
    @PutMapping("/{id}/camisetas")
    public ParticipanteResponseDTO atualizarCamisetas(@PathVariable Integer id,
                                                       @Valid @RequestBody AtualizarCamisetasRequestDTO dto) {
        return pessoaService.atualizarCamisetas(id, dto.camisetas());
    }

    /* Serve o comprovante de pagamento anexado no cadastro — quem confirma
       a inscrição (qualquer papel de comissão, ver SecurityConfig) precisa
       poder ver o que a pessoa enviou antes de aprovar. */
    @GetMapping("/{id}/comprovante")
    public ResponseEntity<Resource> buscarComprovante(@PathVariable Integer id) {
        String nomeArquivo = pessoaService.buscarNomeComprovante(id);
        Path caminho = Paths.get(uploadDir).resolve(nomeArquivo);

        Resource recurso;
        try {
            recurso = new UrlResource(caminho.toUri());
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao ler o comprovante.");
        }
        if (!recurso.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo do comprovante não encontrado.");
        }

        return ResponseEntity.ok()
                .contentType(mediaTypeDoComprovante(nomeArquivo))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nomeArquivo + "\"")
                .body(recurso);
    }

    /* Extensões aceitas no upload (accept="image/*,application/pdf" em
       BoxInscricao.jsx). Extensão fora dessa lista (não deveria acontecer)
       cai em octet-stream — o navegador oferece download em vez de exibir. */
    private MediaType mediaTypeDoComprovante(String nomeArquivo) {
        String extensao = nomeArquivo.substring(nomeArquivo.lastIndexOf('.') + 1).toLowerCase();
        return switch (extensao) {
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "gif" -> MediaType.IMAGE_GIF;
            case "pdf" -> MediaType.APPLICATION_PDF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
