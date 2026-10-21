package com.semac.java_api.service;

import com.semac.java_api.model.Palestrante;
import com.semac.java_api.repository.PalestranteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/* Foto do palestrante (usada no telão do debate). Mesmo padrão da logo de
   patrocinador e da imagem de conquista: grava em pasta própria
   (app.upload.dir.palestrantes) e guarda só o nome do arquivo na coluna.

   Aceita JPG, PNG e WEBP, conferindo os magic bytes — o content-type que o
   navegador manda não basta, e a rota de leitura é pública. */
@Service
public class PalestranteFotoService {

    private final PalestranteRepository palestranteRepository;

    @Value("${app.upload.dir.palestrantes}")
    private String uploadDirPalestrantes;

    public PalestranteFotoService(PalestranteRepository palestranteRepository) {
        this.palestranteRepository = palestranteRepository;
    }

    /* Envia ou substitui a foto. A anterior é apagada: o nome carrega
       timestamp, então ela nunca mais seria referenciada. */
    @Transactional
    public Palestrante salvar(Integer id, MultipartFile arquivo) {
        Palestrante palestrante = buscarOuFalhar(id);

        if (arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhum arquivo enviado.");
        }
        String extensao = extensaoPelosBytes(arquivo);
        if (extensao == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A foto precisa ser JPG, PNG ou WEBP.");
        }

        try {
            Path dir = Paths.get(uploadDirPalestrantes);
            Files.createDirectories(dir);

            String nomeArquivo = "palestrante-" + id + "_" + System.currentTimeMillis() + "." + extensao;
            arquivo.transferTo(dir.resolve(nomeArquivo).toAbsolutePath());

            String anterior = palestrante.getFotoArquivo();
            palestrante.setFotoArquivo(nomeArquivo);
            palestranteRepository.save(palestrante);
            apagarArquivo(anterior);
            return palestrante;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao salvar a foto.");
        }
    }

    @Transactional(readOnly = true)
    public Resource ler(Integer id) {
        String nomeArquivo = buscarOuFalhar(id).getFotoArquivo();
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Este palestrante não tem foto.");
        }

        Resource recurso;
        try {
            recurso = new UrlResource(resolverDentroDaPasta(nomeArquivo).toUri());
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao ler a foto.");
        }
        if (!recurso.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo da foto não encontrado.");
        }
        return recurso;
    }

    public MediaType tipoDaFoto(Resource recurso) {
        String nome = recurso.getFilename() == null ? "" : recurso.getFilename().toLowerCase();
        if (nome.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (nome.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }

    /* Chamado quando o palestrante é excluído (ver EventoService). Falha ao
       apagar não impede nada: arquivo órfão é inofensivo. */
    public void apagarArquivo(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolverDentroDaPasta(nomeArquivo));
        } catch (IOException | ResponseStatusException ignorado) {
            // arquivo órfão é inofensivo
        }
    }

    private Palestrante buscarOuFalhar(Integer id) {
        return palestranteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Palestrante não encontrado."));
    }

    /* JPG: FF D8 FF · PNG: 89 50 4E 47 · WEBP: "RIFF" .... "WEBP" */
    private String extensaoPelosBytes(MultipartFile arquivo) {
        try (InputStream entrada = arquivo.getInputStream()) {
            byte[] b = entrada.readNBytes(12);
            if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
                return "jpg";
            }
            if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
                return "png";
            }
            if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
                return "webp";
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /* O nome em disco é sempre gerado aqui, nunca vem do cliente; a
       checagem fica como na ConquistaController, por garantia. */
    private Path resolverDentroDaPasta(String nomeArquivo) {
        Path dir = Paths.get(uploadDirPalestrantes).normalize();
        Path alvo = dir.resolve(nomeArquivo).normalize();
        if (!alvo.startsWith(dir)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Caminho de foto inválido.");
        }
        return alvo;
    }
}
