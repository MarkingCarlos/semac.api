package com.semac.java_api.controller;

import com.semac.java_api.dto.AtivoRequestDTO;
import com.semac.java_api.dto.MeuTimeDTO;
import com.semac.java_api.dto.MeuTimeResumoDTO;
import com.semac.java_api.dto.TimeNomeRequestDTO;
import com.semac.java_api.dto.TimesPainelDTO;
import com.semac.java_api.model.Time;
import com.semac.java_api.service.TimeService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/* Times da SEMAC.

   Acessos (ver SecurityConfig): `meu` é do participante (página /time);
   o mascote é público porque vai direto num <img src>; o resto é da aba
   Times do /admin, restrito a DIRETOR_SITE/PRESIDENTE. */
@RestController
@RequestMapping("/api/time")
public class TimeController {

    private static final byte[] ASSINATURA_PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };

    /* Teto do mascote — o multipart aceita até 5MB, mas a imagem só ocupa
       um círculo de ~200px na tela do celular. */
    private static final long TAMANHO_MAXIMO_MASCOTE = 2L * 1024 * 1024;

    /* O SVG é texto e pode carregar script. Servido como <img> o navegador
       não executa nada, e a rota pública responde com CSP sandbox (ver
       buscarMascote); ainda assim recusa na entrada o que tem cara de
       script, para que o arquivo em disco nunca seja perigoso por si só. */
    private static final Pattern SVG_PERIGOSO = Pattern.compile(
            "<script|<foreignobject|javascript:|\\son[a-z]+\\s*=", Pattern.CASE_INSENSITIVE);

    private final TimeService timeService;

    @Value("${app.upload.dir.times}")
    private String uploadDirTimes;

    public TimeController(TimeService timeService) {
        this.timeService = timeService;
    }

    /* ── Participante ────────────────────────────────────────────── */

    /* O usuário vem da claim `id` do token — nunca por parâmetro. */
    @GetMapping("/meu")
    public MeuTimeDTO meu(@AuthenticationPrincipal Jwt jwt) {
        return timeService.meuTime(idDoToken(jwt));
    }

    /* Bloco "Seu time" do /participantes. Só leitura: não revela o time
       (isso só acontece em /meu, pela página /time). */
    @GetMapping("/meu/resumo")
    public MeuTimeResumoDTO meuResumo(@AuthenticationPrincipal Jwt jwt) {
        return timeService.resumoMeuTime(idDoToken(jwt));
    }

    /* ── /admin ──────────────────────────────────────────────────── */

    @GetMapping
    public TimesPainelDTO painel() {
        return timeService.painel();
    }

    @PatchMapping("/ativos")
    public TimesPainelDTO definirAtivos(@Valid @RequestBody AtivoRequestDTO dto) {
        return timeService.definirAtivos(dto.ativo());
    }

    @PutMapping("/{id}")
    public TimesPainelDTO renomear(@PathVariable Integer id, @Valid @RequestBody TimeNomeRequestDTO dto) {
        return timeService.renomear(id, dto.nome());
    }

    @PostMapping("/sortear")
    public TimesPainelDTO sortear() {
        return timeService.sortear();
    }

    /* ── Mascote ─────────────────────────────────────────────────── */

    /* Mesmo padrão da imagem de conquista: grava em pasta própria, guarda
       só o nome do arquivo e apaga o anterior. Aceita PNG ou SVG,
       conferindo o conteúdo — o Content-Type do multipart é palpite do
       cliente. */
    @PostMapping("/{id}/mascote")
    public TimesPainelDTO enviarMascote(@PathVariable Integer id,
                                        @RequestParam("arquivo") MultipartFile arquivo) {
        timeService.buscarOuFalhar(id);

        if (arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhum arquivo enviado.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_MASCOTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O mascote pode ter no máximo 2MB.");
        }

        String extensao;
        if (ehPng(arquivo)) {
            extensao = "png";
        } else if (ehSvg(arquivo)) {
            extensao = "svg";
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O mascote precisa ser um PNG ou um SVG sem scripts.");
        }

        try {
            Path dir = Paths.get(uploadDirTimes);
            Files.createDirectories(dir);

            String nomeArquivo = "time-" + id + "_" + System.currentTimeMillis() + "." + extensao;
            arquivo.transferTo(dir.resolve(nomeArquivo));

            String anterior = timeService.definirMascote(id, nomeArquivo);
            if (anterior != null && !anterior.isBlank() && !anterior.equals(nomeArquivo)) {
                try {
                    Files.deleteIfExists(resolverDentroDaPasta(dir, anterior));
                } catch (IOException ignorado) {
                    // arquivo órfão é inofensivo; o upload novo já valeu
                }
            }
            return timeService.painel();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao salvar o mascote.");
        }
    }

    /* Pública: usada direto num <img src> em /time e no preview do /admin. */
    @GetMapping("/{id}/mascote")
    public ResponseEntity<Resource> buscarMascote(@PathVariable Integer id) {
        Time time = timeService.buscarOuFalhar(id);

        String nomeArquivo = time.getMascoteArquivo();
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Este time não tem mascote.");
        }

        Path caminho = resolverDentroDaPasta(Paths.get(uploadDirTimes), nomeArquivo);
        Resource recurso;
        try {
            recurso = new UrlResource(caminho.toUri());
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao ler o mascote.");
        }
        if (!recurso.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo do mascote não encontrado.");
        }

        MediaType tipo = nomeArquivo.toLowerCase(Locale.ROOT).endsWith(".svg")
                ? MediaType.valueOf("image/svg+xml")
                : MediaType.IMAGE_PNG;

        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .header("X-Content-Type-Options", "nosniff")
                /* Se alguém abrir o SVG direto na aba (fora de um <img>),
                   o sandbox impede qualquer script de rodar na origem da API. */
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox")
                .body(recurso);
    }

    /* ── Auxiliares ──────────────────────────────────────────────── */

    private boolean ehPng(MultipartFile arquivo) {
        try (InputStream entrada = arquivo.getInputStream()) {
            byte[] inicio = entrada.readNBytes(ASSINATURA_PNG.length);
            return Arrays.equals(inicio, ASSINATURA_PNG);
        } catch (IOException e) {
            return false;
        }
    }

    /* SVG válido aqui: texto UTF-8 com uma tag <svg e nada que pareça script. */
    private boolean ehSvg(MultipartFile arquivo) {
        try (InputStream entrada = arquivo.getInputStream()) {
            String conteudo = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            String inicio = conteudo.replace("﻿", "").stripLeading();
            boolean pareceSvg = inicio.startsWith("<") && conteudo.toLowerCase(Locale.ROOT).contains("<svg");
            return pareceSvg && !SVG_PERIGOSO.matcher(conteudo).find();
        } catch (IOException e) {
            return false;
        }
    }

    /* O nome em disco é sempre gerado aqui (time-{id}_{ts}.ext), mas a
       checagem fica como rede de segurança contra "../". */
    private Path resolverDentroDaPasta(Path dir, String nomeArquivo) {
        Path alvo = dir.resolve(nomeArquivo).normalize();
        if (!alvo.startsWith(dir.normalize())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Caminho de mascote inválido.");
        }
        return alvo;
    }

    private Integer idDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (id instanceof Number numero) {
            return numero.intValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
    }
}
