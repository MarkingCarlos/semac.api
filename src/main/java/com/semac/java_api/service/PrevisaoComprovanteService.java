package com.semac.java_api.service;

import com.semac.java_api.dto.ComprovantePrevisaoResponseDTO;
import com.semac.java_api.model.PrevisaoItem;
import com.semac.java_api.model.PrevisaoItemComprovante;
import com.semac.java_api.repository.PrevisaoItemComprovanteRepository;
import com.semac.java_api.repository.PrevisaoItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/* Comprovantes de compra anexados aos itens da previsão.

   Mesmo padrão de armazenamento do logo de patrocinador e da imagem de
   conquista: o arquivo vai para uma pasta própria em disco e o banco
   guarda só o nome gerado aqui. A diferença é que cada item pode ter
   vários comprovantes, por isso eles ganham tabela própria em vez de uma
   coluna no item. */
@Service
public class PrevisaoComprovanteService {

    /* 5 MB, igual ao spring.servlet.multipart.max-file-size. Conferido
       aqui também para a mensagem de erro ser a nossa, e não a do Spring. */
    private static final long TAMANHO_MAXIMO_BYTES = 5L * 1024 * 1024;

    private static final byte[] ASSINATURA_PDF = { '%', 'P', 'D', 'F' };
    private static final byte[] ASSINATURA_JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
    private static final byte[] ASSINATURA_PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };
    private static final byte[] ASSINATURA_RIFF = { 'R', 'I', 'F', 'F' };
    private static final byte[] ASSINATURA_WEBP = { 'W', 'E', 'B', 'P' };

    private final PrevisaoItemRepository itemRepository;
    private final PrevisaoItemComprovanteRepository comprovanteRepository;

    @Value("${app.upload.dir.comprovantes-previsao}")
    private String uploadDirComprovantes;

    public PrevisaoComprovanteService(PrevisaoItemRepository itemRepository,
                                      PrevisaoItemComprovanteRepository comprovanteRepository) {
        this.itemRepository = itemRepository;
        this.comprovanteRepository = comprovanteRepository;
    }

    public List<ComprovantePrevisaoResponseDTO> listar(Integer itemId) {
        buscarItemOuFalhar(itemId);
        return comprovanteRepository.findAllByPrevisaoItem_IdOrderByEnviadoEmAscIdAsc(itemId).stream()
                .map(this::paraResposta)
                .toList();
    }

    public ComprovantePrevisaoResponseDTO anexar(Integer itemId, MultipartFile arquivo) {
        PrevisaoItem item = buscarItemOuFalhar(itemId);

        if (arquivo == null || arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhum arquivo enviado.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O comprovante passa de 5 MB.");
        }

        /* O tipo sai dos magic bytes, não do Content-Type do multipart —
           esse é palpite do cliente e um arquivo renomeado passaria. */
        TipoComprovante tipo = detectarTipo(arquivo);
        if (tipo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato não aceito. Envie PDF, JPG, PNG ou WEBP.");
        }

        String nomeArquivo = "previsao-" + itemId + "_" + UUID.randomUUID() + tipo.extensao;
        Path dir = Paths.get(uploadDirComprovantes);
        try {
            Files.createDirectories(dir);
            arquivo.transferTo(resolverDentroDaPasta(dir, nomeArquivo));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao salvar o comprovante.");
        }

        PrevisaoItemComprovante comprovante = new PrevisaoItemComprovante();
        comprovante.setPrevisaoItem(item);
        comprovante.setNomeOriginal(limparNomeOriginal(arquivo.getOriginalFilename(), tipo));
        comprovante.setNomeArquivo(nomeArquivo);
        comprovante.setTipoConteudo(tipo.contentType);
        comprovante.setTamanhoBytes(arquivo.getSize());
        comprovante.setEnviadoEm(LocalDateTime.now());

        try {
            return paraResposta(comprovanteRepository.save(comprovante));
        } catch (RuntimeException e) {
            // Sem a linha no banco o arquivo nunca seria referenciado.
            apagarArquivoSilenciosamente(nomeArquivo);
            throw e;
        }
    }

    public PrevisaoItemComprovante buscarOuFalhar(Integer itemId, Integer comprovanteId) {
        return comprovanteRepository.findByIdAndPrevisaoItem_Id(comprovanteId, itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprovante não encontrado."));
    }

    public Resource lerArquivo(PrevisaoItemComprovante comprovante) {
        Path caminho = resolverDentroDaPasta(Paths.get(uploadDirComprovantes), comprovante.getNomeArquivo());
        Resource recurso;
        try {
            recurso = new UrlResource(caminho.toUri());
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao ler o comprovante.");
        }
        if (!recurso.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo do comprovante não encontrado.");
        }
        return recurso;
    }

    public void excluir(Integer itemId, Integer comprovanteId) {
        PrevisaoItemComprovante comprovante = buscarOuFalhar(itemId, comprovanteId);
        comprovanteRepository.delete(comprovante);
        apagarArquivoSilenciosamente(comprovante.getNomeArquivo());
    }

    /* Nomes em disco dos comprovantes do item — o PrevisaoItemController
       lê antes de excluir o item (o CASCADE do banco leva as linhas, mas
       não os arquivos) e apaga depois com apagarArquivos. */
    public List<String> nomesArquivosDoItem(Integer itemId) {
        return comprovanteRepository.findAllByPrevisaoItem_IdOrderByEnviadoEmAscIdAsc(itemId).stream()
                .map(PrevisaoItemComprovante::getNomeArquivo)
                .toList();
    }

    public void apagarArquivos(List<String> nomesArquivos) {
        nomesArquivos.forEach(this::apagarArquivoSilenciosamente);
    }

    public long contar(Integer itemId) {
        return comprovanteRepository.countByPrevisaoItem_Id(itemId);
    }

    public Map<Integer, Long> contarPorItem() {
        Map<Integer, Long> totais = new HashMap<>();
        for (Object[] linha : comprovanteRepository.contarPorItem()) {
            totais.put((Integer) linha[0], (Long) linha[1]);
        }
        return totais;
    }

    private PrevisaoItem buscarItemOuFalhar(Integer itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Previsão não encontrada."));
    }

    private ComprovantePrevisaoResponseDTO paraResposta(PrevisaoItemComprovante comprovante) {
        return new ComprovantePrevisaoResponseDTO(
                comprovante.getId(),
                comprovante.getNomeOriginal(),
                comprovante.getTipoConteudo(),
                comprovante.getTamanhoBytes(),
                comprovante.getEnviadoEm());
    }

    private TipoComprovante detectarTipo(MultipartFile arquivo) {
        byte[] inicio;
        try (InputStream entrada = arquivo.getInputStream()) {
            inicio = entrada.readNBytes(12);
        } catch (IOException e) {
            return null;
        }
        if (comecaCom(inicio, 0, ASSINATURA_PDF)) return TipoComprovante.PDF;
        if (comecaCom(inicio, 0, ASSINATURA_JPEG)) return TipoComprovante.JPEG;
        if (comecaCom(inicio, 0, ASSINATURA_PNG)) return TipoComprovante.PNG;
        if (comecaCom(inicio, 0, ASSINATURA_RIFF) && comecaCom(inicio, 8, ASSINATURA_WEBP)) return TipoComprovante.WEBP;
        return null;
    }

    private boolean comecaCom(byte[] dados, int deslocamento, byte[] assinatura) {
        if (dados.length < deslocamento + assinatura.length) return false;
        return Arrays.equals(dados, deslocamento, deslocamento + assinatura.length,
                assinatura, 0, assinatura.length);
    }

    /* O nome original só é exibido e devolvido no download, mas vem do
       cliente: tira caminho, caracteres de controle e limita o tamanho. */
    private String limparNomeOriginal(String nomeOriginal, TipoComprovante tipo) {
        String nome = nomeOriginal == null ? "" : nomeOriginal;
        nome = nome.substring(Math.max(nome.lastIndexOf('/'), nome.lastIndexOf('\\')) + 1);
        nome = nome.replaceAll("\\p{Cntrl}", "").trim();
        if (nome.isEmpty()) {
            nome = "comprovante" + tipo.extensao;
        }
        return nome.length() > 255 ? nome.substring(nome.length() - 255) : nome;
    }

    private void apagarArquivoSilenciosamente(String nomeArquivo) {
        try {
            Files.deleteIfExists(resolverDentroDaPasta(Paths.get(uploadDirComprovantes), nomeArquivo));
        } catch (IOException | ResponseStatusException ignorado) {
            // arquivo órfão é inofensivo; o registro já foi removido
        }
    }

    /* O nome em disco é sempre gerado aqui e nunca vem do cliente; a
       checagem fica pelo mesmo motivo do ConquistaController. */
    private Path resolverDentroDaPasta(Path dir, String nomeArquivo) {
        Path alvo = dir.resolve(nomeArquivo).normalize();
        if (!alvo.startsWith(dir.normalize())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Caminho de comprovante inválido.");
        }
        return alvo;
    }

    private enum TipoComprovante {
        PDF("application/pdf", ".pdf"),
        JPEG("image/jpeg", ".jpg"),
        PNG("image/png", ".png"),
        WEBP("image/webp", ".webp");

        final String contentType;
        final String extensao;

        TipoComprovante(String contentType, String extensao) {
            this.contentType = contentType;
            this.extensao = extensao;
        }
    }
}
