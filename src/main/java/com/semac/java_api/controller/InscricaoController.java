package com.semac.java_api.controller;

import com.semac.java_api.dto.InscricaoRequestDTO;
import com.semac.java_api.dto.PessoaResponseDTO;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.service.InscricaoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/inscricao")
public class InscricaoController {

    private final InscricaoService inscricaoService;
    private final PessoaRepository pessoaRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public InscricaoController(InscricaoService inscricaoService, PessoaRepository pessoaRepository) {
        this.inscricaoService = inscricaoService;
        this.pessoaRepository = pessoaRepository;
    }

    @PostMapping
    public ResponseEntity<PessoaResponseDTO> cadastrar(@Valid @RequestBody InscricaoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inscricaoService.cadastrar(dto));
    }

    @PostMapping("/{uuid}/comprovante")
    public ResponseEntity<Map<String, String>> uploadComprovante(
            @PathVariable String uuid,
            @RequestParam("arquivo") MultipartFile arquivo) {

        Pessoa pessoa = pessoaRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscrição não encontrada."));

        try {
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);

            String original = arquivo.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.'))
                    : "";
            String nomeArquivo = uuid + "_" + System.currentTimeMillis() + ext;

            arquivo.transferTo(dir.resolve(nomeArquivo));

            pessoa.setComprovantePagamento(nomeArquivo);
            pessoaRepository.save(pessoa);

            return ResponseEntity.ok(Map.of("caminho", nomeArquivo));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao salvar o comprovante.");
        }
    }
}
