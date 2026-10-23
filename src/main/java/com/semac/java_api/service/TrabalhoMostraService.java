package com.semac.java_api.service;

import com.semac.java_api.dto.MostraProgressoDTO;
import com.semac.java_api.dto.TrabalhoMostraAdminDTO;
import com.semac.java_api.dto.TrabalhoMostraParticipanteDTO;
import com.semac.java_api.dto.TrabalhoMostraRequestDTO;
import com.semac.java_api.dto.VisitaMostraRespostaDTO;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.TrabalhoMostra;
import com.semac.java_api.model.VisitaTrabalhoMostra;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.repository.TrabalhoMostraRepository;
import com.semac.java_api.repository.VisitaTrabalhoMostraRepository;
import com.semac.java_api.repository.projection.ContagemVisitasTrabalhoMostraView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/* Mostra Técnica: os trabalhos dos stands e as visitas dos participantes.

   Cada stand tem um QR code impresso com o link /mostra/<codigoQr>. O
   participante lê com a câmera do celular, cai no site logado e a visita
   é registrada aqui. Quem visita todos os trabalhos da edição ganha a
   conquista MOSTRA_COMPLETA na hora da última leitura (ver
   ConquistaService.avaliarMostraCompleta).

   Risco aceito: o QR pode ser fotografado e compartilhado. Não há janela
   de horário nem código rotativo — foi decisão da organização.

   A presença na Mostra NÃO passa por aqui: continua sendo o check-in da
   comissão na entrada (evento MOSTRA_TECNICA). */
@Service
public class TrabalhoMostraService {

    private static final String ALFABETO_CODIGO_QR = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int TAMANHO_CODIGO_QR = 12;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final TrabalhoMostraRepository trabalhoRepository;
    private final VisitaTrabalhoMostraRepository visitaRepository;
    private final PessoaRepository pessoaRepository;
    private final ConquistaService conquistaService;

    public TrabalhoMostraService(TrabalhoMostraRepository trabalhoRepository,
                                 VisitaTrabalhoMostraRepository visitaRepository,
                                 PessoaRepository pessoaRepository,
                                 ConquistaService conquistaService) {
        this.trabalhoRepository = trabalhoRepository;
        this.visitaRepository = visitaRepository;
        this.pessoaRepository = pessoaRepository;
        this.conquistaService = conquistaService;
    }

    /* ── Participante ────────────────────────────────────────────── */

    /* Registra a leitura do QR de um stand. Ler de novo não é erro: volta
       `jaVisitado` e o progresso, para a tela só trocar o texto. */
    @Transactional
    public VisitaMostraRespostaDTO visitar(Integer pessoaId, String codigoQr) {
        TrabalhoMostra trabalho = trabalhoRepository.findByCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Este QR code não é de nenhum trabalho da Mostra Técnica."));
        Pessoa pessoa = pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));

        boolean jaVisitado = visitaRepository.existsByPessoaIdAndTrabalhoId(pessoaId, trabalho.getId());
        boolean conquistaConcedida = false;
        if (!jaVisitado) {
            VisitaTrabalhoMostra visita = new VisitaTrabalhoMostra();
            visita.setPessoa(pessoa);
            visita.setTrabalho(trabalho);
            visita.setVisitadaEm(LocalDateTime.now());
            visitaRepository.save(visita);
            conquistaConcedida = conquistaService.avaliarMostraCompleta(pessoa, trabalho.getAno());
        }

        return new VisitaMostraRespostaDTO(
                trabalho.getTitulo(),
                trabalho.getApresentadores(),
                jaVisitado,
                (int) visitaRepository.countByPessoaIdAndTrabalhoAno(pessoaId, trabalho.getAno()),
                (int) trabalhoRepository.countByAno(trabalho.getAno()),
                conquistaConcedida);
    }

    /* O progresso da pessoa na edição atual — o card da aba Desafios. */
    @Transactional(readOnly = true)
    public MostraProgressoDTO progresso(Integer pessoaId) {
        int ano = LocalDate.now().getYear();
        Set<Integer> visitados = new HashSet<>(visitaRepository.idsTrabalhosVisitados(pessoaId, ano));
        List<TrabalhoMostraParticipanteDTO> trabalhos = trabalhoRepository.findByAnoOrderByIdAsc(ano).stream()
                .map(t -> new TrabalhoMostraParticipanteDTO(
                        t.getId(), t.getTitulo(), t.getApresentadores(),
                        visitados.contains(t.getId())))
                .toList();
        int totalVisitados = (int) trabalhos.stream().filter(TrabalhoMostraParticipanteDTO::visitado).count();
        return new MostraProgressoDTO(totalVisitados, trabalhos.size(), trabalhos);
    }

    /* ── Diretoria (/admin -> Mostra Técnica) ────────────────────── */

    @Transactional(readOnly = true)
    public List<TrabalhoMostraAdminDTO> listarParaAdmin(Integer ano) {
        List<TrabalhoMostra> trabalhos = trabalhoRepository.findByAnoOrderByIdAsc(ano);
        Map<Integer, Long> visitas = trabalhos.isEmpty() ? Map.of()
                : visitaRepository.contarVisitasPorTrabalho(trabalhos.stream().map(TrabalhoMostra::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(ContagemVisitasTrabalhoMostraView::getTrabalhoId,
                                ContagemVisitasTrabalhoMostraView::getTotal));
        return trabalhos.stream()
                .map(t -> paraAdmin(t, visitas.getOrDefault(t.getId(), 0L)))
                .toList();
    }

    @Transactional
    public TrabalhoMostraAdminDTO criar(TrabalhoMostraRequestDTO dto) {
        TrabalhoMostra trabalho = new TrabalhoMostra();
        trabalho.setAno(dto.ano());
        aplicar(trabalho, dto);
        trabalho.setCodigoQr(gerarCodigoQrUnico());
        trabalho.setCriadoEm(LocalDateTime.now());
        return paraAdmin(trabalhoRepository.save(trabalho), 0L);
    }

    /* O código do QR não muda na edição: o QR já impresso no stand
       continua valendo. */
    @Transactional
    public TrabalhoMostraAdminDTO atualizar(Integer id, TrabalhoMostraRequestDTO dto) {
        TrabalhoMostra trabalho = buscarOuFalhar(id);
        aplicar(trabalho, dto);
        trabalhoRepository.save(trabalho);
        long visitas = visitaRepository.contarVisitasPorTrabalho(List.of(id)).stream()
                .findFirst().map(ContagemVisitasTrabalhoMostraView::getTotal).orElse(0L);
        return paraAdmin(trabalho, visitas);
    }

    /* Remove o trabalho e, pelo ON DELETE CASCADE, as visitas a ele.
       Conquista já concedida não é revogada. */
    @Transactional
    public void remover(Integer id) {
        trabalhoRepository.delete(buscarOuFalhar(id));
    }

    /* ── Auxiliares ──────────────────────────────────────────────── */

    private void aplicar(TrabalhoMostra trabalho, TrabalhoMostraRequestDTO dto) {
        trabalho.setTitulo(dto.titulo().trim());
        trabalho.setApresentadores(dto.apresentadores().trim());
    }

    private TrabalhoMostra buscarOuFalhar(Integer id) {
        return trabalhoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trabalho não encontrado."));
    }

    /* Sem caracteres que se confundem (0/O, 1/l/I) — o link às vezes é
       digitado à mão quando a câmera não lê. */
    private String gerarCodigoQrUnico() {
        String codigo;
        do {
            StringBuilder construtor = new StringBuilder(TAMANHO_CODIGO_QR);
            for (int i = 0; i < TAMANHO_CODIGO_QR; i++) {
                construtor.append(ALFABETO_CODIGO_QR.charAt(ALEATORIO.nextInt(ALFABETO_CODIGO_QR.length())));
            }
            codigo = construtor.toString();
        } while (trabalhoRepository.existsByCodigoQr(codigo));
        return codigo;
    }

    private TrabalhoMostraAdminDTO paraAdmin(TrabalhoMostra trabalho, long visitas) {
        return new TrabalhoMostraAdminDTO(
                trabalho.getId(),
                trabalho.getTitulo(),
                trabalho.getApresentadores(),
                trabalho.getCodigoQr(),
                visitas);
    }
}
