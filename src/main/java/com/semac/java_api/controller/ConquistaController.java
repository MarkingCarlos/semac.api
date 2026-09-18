package com.semac.java_api.controller;

import com.semac.java_api.dto.AtivoRequestDTO;
import com.semac.java_api.dto.ConcederConquistaRequestDTO;
import com.semac.java_api.dto.ConquistaConcedidaDTO;
import com.semac.java_api.dto.ConquistaParticipanteDTO;
import com.semac.java_api.dto.ConquistaRequestDTO;
import com.semac.java_api.dto.ConquistaResponseDTO;
import com.semac.java_api.model.Conquista;
import com.semac.java_api.service.ConquistaService;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/* Catálogo de conquistas.

   As conquistas não são criadas nem excluídas por aqui: cada uma nasce de
   uma entrada em CatalogoConquistas e é materializada no boot pelo
   ConquistaSeedRunner. O que este controller expõe é a metade editável —
   nome, pontos, descrição, raridade, ordem, imagem e o interruptor `ativa`
   — mais a vitrine do participante.

   Acessos (ver SecurityConfig): a imagem é pública porque vai direto num
   <img src>; `minhas` é do participante; a leitura do catálogo é de
   qualquer comissão; a escrita é de DIRETOR_SITE/PRESIDENTE, mesmo público
   dos níveis e cotas. */
@RestController
@RequestMapping("/api/conquista")
public class ConquistaController {

    /* O card do participante é quadrado e o efeito preto e branco depende
       de fundo transparente, então PNG é o único formato aceito — decisão
       de produto, não limitação técnica. */
    private static final String TIPO_IMAGEM_ACEITO = "image/png";

    /* Assinatura de um arquivo PNG. O Content-Type do multipart é enviado
       pelo cliente, ou seja, é palpite dele — conferir os magic bytes é o
       que de fato garante que o que entrou é uma imagem, e não outra coisa
       renomeada. */
    private static final byte[] ASSINATURA_PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };

    private final ConquistaService conquistaService;

    @Value("${app.upload.dir.conquistas}")
    private String uploadDirConquistas;

    public ConquistaController(ConquistaService conquistaService) {
        this.conquistaService = conquistaService;
    }

    /* ── Vitrine do participante ─────────────────────────────────── */

    /* Conquistas ativas do próprio participante logado, marcando quais ele
       já tem. O usuário vem da claim `id` do token — nunca por parâmetro,
       mesmo critério de GET /api/pessoa/me. */
    @GetMapping("/minhas")
    public List<ConquistaParticipanteDTO> minhasConquistas(@AuthenticationPrincipal Jwt jwt) {
        return conquistaService.listarDoParticipante(idDoToken(jwt));
    }

    /* Registra que a animação de desbloqueio desta conquista já foi
       exibida ao participante logado, para não repetir na próxima
       abertura. Chamada no fim da animação de cada conquista.

       Devolve 204 mesmo quando não havia nada a marcar: é uma confirmação
       de exibição, não uma operação com resultado, e o front não tem o que
       fazer com um erro aqui. */
    @PostMapping("/{id}/vista")
    public ResponseEntity<Void> marcarComoVista(@PathVariable Integer id,
                                                @AuthenticationPrincipal Jwt jwt) {
        conquistaService.marcarComoVista(idDoToken(jwt), id);
        return ResponseEntity.noContent().build();
    }

    /* ── Catálogo (/admin -> Informações SEMAC) ──────────────────── */

    @GetMapping
    public List<ConquistaResponseDTO> listar() {
        return conquistaService.listarCatalogo();
    }

    @PutMapping("/{id}")
    public ConquistaResponseDTO atualizar(@PathVariable Integer id,
                                          @Valid @RequestBody ConquistaRequestDTO dto) {
        return conquistaService.atualizar(id, dto);
    }

    /* Liga/desliga a conquista. Devolve 409 com o motivo quando barrado —
       sem imagem para ativar, ou com participantes vinculados para
       desativar (ver ConquistaService.alterarAtiva). */
    @PatchMapping("/{id}/ativa")
    public ConquistaResponseDTO alterarAtiva(@PathVariable Integer id,
                                             @Valid @RequestBody AtivoRequestDTO dto) {
        return conquistaService.alterarAtiva(id, dto.ativo());
    }

    /* Reavalia as conquistas automáticas de todo participante. É a saída
       durante a semana do evento: ativada uma conquista, este botão a faz
       alcançar quem já cumpriu a regra, sem reiniciar a API. */
    @PostMapping("/reavaliar")
    public Map<String, Integer> reavaliar() {
        return Map.of("participantesAvaliados", conquistaService.reavaliarAutomaticasDeTodos());
    }

    /* ── Concessão manual (/checkin) ─────────────────────────────── */

    /* Concede uma conquista MANUAL lendo o QR do crachá (uuid) ou pelo id,
       quando a leitura falha. Quem concede sai da claim do token e fica
       gravado no vínculo. Restrito a diretores e presidência — MEMBRO tem
       acesso ao /checkin para marcar presença, mas não para creditar
       pontos (ver SecurityConfig). */
    @PostMapping("/{id}/conceder")
    public ConquistaConcedidaDTO conceder(@PathVariable Integer id,
                                          @RequestBody ConcederConquistaRequestDTO dto,
                                          @AuthenticationPrincipal Jwt jwt) {
        return conquistaService.concederManualmente(id, dto, idDoToken(jwt));
    }

    /* ── Imagem ──────────────────────────────────────────────────── */

    /* Mesmo padrão do logo de patrocinador (ver PatrocinadorController):
       grava em pasta própria e guarda só o nome do arquivo na coluna.
       Aceita apenas PNG. */
    @PostMapping("/{id}/imagem")
    public ConquistaResponseDTO enviarImagem(@PathVariable Integer id,
                                             @RequestParam("arquivo") MultipartFile arquivo) {
        Conquista conquista = conquistaService.buscarOuFalhar(id);

        if (arquivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhum arquivo enviado.");
        }
        if (!TIPO_IMAGEM_ACEITO.equalsIgnoreCase(arquivo.getContentType()) || !ehPng(arquivo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A imagem da conquista precisa ser um PNG.");
        }

        try {
            Path dir = Paths.get(uploadDirConquistas);
            Files.createDirectories(dir);

            String nomeArquivo = "conquista-" + id + "_" + System.currentTimeMillis() + ".png";
            arquivo.transferTo(dir.resolve(nomeArquivo));

            /* Troca de imagem apaga a anterior: o nome carrega timestamp,
               então o arquivo velho nunca mais seria referenciado e só
               ocuparia espaço. Falha ao apagar não invalida o upload. */
            String anterior = conquista.getImagemUrl();
            ConquistaResponseDTO resposta = conquistaService.definirImagem(id, nomeArquivo);
            if (anterior != null && !anterior.isBlank() && !anterior.equals(nomeArquivo)) {
                try {
                    Files.deleteIfExists(dir.resolve(anterior));
                } catch (IOException ignorado) {
                    // arquivo órfão é inofensivo; o upload novo já valeu
                }
            }
            return resposta;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao salvar a imagem.");
        }
    }

    /* Pública: usada direto num <img src> na área do participante e no
       preview do /admin. */
    @GetMapping("/{id}/imagem")
    public ResponseEntity<Resource> buscarImagem(@PathVariable Integer id) {
        Conquista conquista = conquistaService.buscarOuFalhar(id);

        String nomeArquivo = conquista.getImagemUrl();
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Esta conquista não tem imagem.");
        }

        Path caminho = resolverDentroDaPasta(Paths.get(uploadDirConquistas), nomeArquivo);
        Resource recurso;
        try {
            recurso = new UrlResource(caminho.toUri());
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao ler a imagem.");
        }
        if (!recurso.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo da imagem não encontrado.");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                /* Impede o navegador de "adivinhar" outro tipo a partir do
                   conteúdo — sem isso, um arquivo forjado servido como PNG
                   poderia ser reinterpretado como HTML e virar XSS numa
                   rota que é pública. */
                .header("X-Content-Type-Options", "nosniff")
                .body(recurso);
    }

    /* Confere os magic bytes do arquivo enviado. */
    private boolean ehPng(MultipartFile arquivo) {
        try (InputStream entrada = arquivo.getInputStream()) {
            byte[] inicio = entrada.readNBytes(ASSINATURA_PNG.length);
            return Arrays.equals(inicio, ASSINATURA_PNG);
        } catch (IOException e) {
            return false;
        }
    }

    /* O nome do arquivo em disco é sempre gerado aqui (conquista-{id}_{ts}.png)
       e nunca vem do cliente, então não há como injetar "../" por essa via.
       Ainda assim a checagem fica: se um dia a origem do nome mudar, o
       problema aparece como 500 aqui em vez de leitura de arquivo arbitrário. */
    private Path resolverDentroDaPasta(Path dir, String nomeArquivo) {
        Path alvo = dir.resolve(nomeArquivo).normalize();
        if (!alvo.startsWith(dir.normalize())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Caminho de imagem inválido.");
        }
        return alvo;
    }

    /* Extrai o id da pessoa da claim `id` do token (gravada no login) —
       mesmo auxiliar de PessoaController. */
    private Integer idDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (id instanceof Number numero) {
            return numero.intValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
    }
}
