package com.semac.java_api.service;

import com.semac.java_api.dto.LaboratorioAdminDTO;
import com.semac.java_api.dto.LaboratorioRequestDTO;
import com.semac.java_api.dto.QuizLaboratorioDTO;
import com.semac.java_api.dto.RespostaQuizLaboratorioRequestDTO;
import com.semac.java_api.dto.ResultadoQuizLaboratorioDTO;
import com.semac.java_api.model.AlternativaPerguntaLaboratorio;
import com.semac.java_api.model.Laboratorio;
import com.semac.java_api.model.PerguntaLaboratorio;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.RespostaQuizLaboratorio;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.repository.LaboratorioRepository;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.repository.RespostaQuizLaboratorioRepository;
import com.semac.java_api.repository.projection.ContagemRespostasLaboratorioView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/* Quiz dos Laboratórios (Mostra Técnica de 29/10).

   A comissão cadastra os laboratórios e as perguntas no /admin; cada um
   tem um QR com o link /laboratorio/<codigoQr>. O participante lê com a
   câmera do celular, cai no site logado e responde.

   Regras, todas aqui (a tela só exibe):
   - o quiz só aceita resposta com `quizAberto` (a comissão libera na hora);
   - uma tentativa por pessoa por laboratório (ver
     RespostaQuizLaboratorioRepository.registrar), corrigida no servidor —
     a marca de correta nunca vai para o participante antes do envio;
   - quem acerta todas ganha XP_GABARITO_LABORATORIO no ranking, uma vez
     por laboratório;
   - editar as perguntas depois não mexe em placar nem em XP já dados.

   Risco aceito, igual ao da Mostra: o QR pode ser fotografado e
   compartilhado. */
@Service
public class LaboratorioQuizService {

    /* XP de quem gabarita o quiz de um laboratório. */
    static final int XP_GABARITO_LABORATORIO = 2;

    private static final String ALFABETO_CODIGO_QR = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int TAMANHO_CODIGO_QR = 12;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final LaboratorioRepository laboratorioRepository;
    private final RespostaQuizLaboratorioRepository respostaRepository;
    private final PessoaRepository pessoaRepository;
    private final XpService xpService;

    public LaboratorioQuizService(LaboratorioRepository laboratorioRepository,
                                  RespostaQuizLaboratorioRepository respostaRepository,
                                  PessoaRepository pessoaRepository,
                                  XpService xpService) {
        this.laboratorioRepository = laboratorioRepository;
        this.respostaRepository = respostaRepository;
        this.pessoaRepository = pessoaRepository;
        this.xpService = xpService;
    }

    /* ── Participante ────────────────────────────────────────────── */

    /* O que a tela mostra ao ler o QR: as perguntas sem gabarito, ou o
       placar de quem já respondeu, ou o aviso de quiz fechado. */
    @Transactional(readOnly = true)
    public QuizLaboratorioDTO abrirQuiz(Integer pessoaId, String codigoQr) {
        Laboratorio laboratorio = buscarPorCodigo(codigoQr);
        ResultadoQuizLaboratorioDTO resultado = respostaRepository
                .findByPessoaIdAndLaboratorioId(pessoaId, laboratorio.getId())
                .map(LaboratorioQuizService::placar)
                .orElse(null);

        boolean aberto = Boolean.TRUE.equals(laboratorio.getQuizAberto());
        List<QuizLaboratorioDTO.Pergunta> perguntas = (aberto && resultado == null)
                ? perguntasSemGabarito(laboratorio)
                : List.of();

        return new QuizLaboratorioDTO(laboratorio.getNome(), laboratorio.getResponsaveis(),
                aberto, perguntas, resultado);
    }

    /* Corrige e grava a tentativa única. Precisa de uma escolha para cada
       pergunta; a correção volta junto para a tela de resultado. */
    @Transactional
    public ResultadoQuizLaboratorioDTO responder(Integer pessoaId, String codigoQr,
                                                 RespostaQuizLaboratorioRequestDTO dto) {
        Laboratorio laboratorio = buscarPorCodigo(codigoQr);
        if (!Boolean.TRUE.equals(laboratorio.getQuizAberto())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O quiz deste laboratório não está aberto agora.");
        }
        List<PerguntaLaboratorio> perguntas = laboratorio.getPerguntas();
        if (perguntas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este laboratório ainda não tem perguntas.");
        }
        Pessoa pessoa = buscarParticipanteAtivo(pessoaId);
        if (respostaRepository.findByPessoaIdAndLaboratorioId(pessoaId, laboratorio.getId()).isPresent()) {
            throw jaRespondeu();
        }

        List<ResultadoQuizLaboratorioDTO.Correcao> correcao = corrigir(perguntas, dto);
        int acertos = (int) correcao.stream().filter(ResultadoQuizLaboratorioDTO.Correcao::acertou).count();
        int total = perguntas.size();
        boolean gabaritou = acertos == total;
        int xp = gabaritou ? XP_GABARITO_LABORATORIO : 0;

        int gravadas = respostaRepository.registrar(pessoaId, laboratorio.getId(), acertos, total, xp, LocalDateTime.now());
        if (gravadas == 0) {
            // Outro envio da mesma pessoa chegou entre a verificação e a gravação.
            throw jaRespondeu();
        }
        if (xp > 0) {
            xpService.creditar(pessoa, xp);
        }
        return new ResultadoQuizLaboratorioDTO(acertos, total, gabaritou, xp, correcao);
    }

    /* ── Comissão: modo teste ────────────────────────────────────── */

    /* O quiz como o participante vê, aberto ou não — a comissão testa
       antes de liberar. Sem gabarito, igual ao participante: quem precisa
       conferir a correta usa o editor. */
    @Transactional(readOnly = true)
    public QuizLaboratorioDTO abrirTeste(Integer id) {
        Laboratorio laboratorio = buscarOuFalhar(id);
        return new QuizLaboratorioDTO(laboratorio.getNome(), laboratorio.getResponsaveis(),
                true, perguntasSemGabarito(laboratorio), null);
    }

    /* Corrige como o envio de verdade, mas não grava tentativa nem
       credita xp: dá para refazer quantas vezes quiser. */
    @Transactional(readOnly = true)
    public ResultadoQuizLaboratorioDTO responderTeste(Integer id, RespostaQuizLaboratorioRequestDTO dto) {
        Laboratorio laboratorio = buscarOuFalhar(id);
        List<PerguntaLaboratorio> perguntas = laboratorio.getPerguntas();
        if (perguntas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este laboratório ainda não tem perguntas.");
        }
        List<ResultadoQuizLaboratorioDTO.Correcao> correcao = corrigir(perguntas, dto);
        int acertos = (int) correcao.stream().filter(ResultadoQuizLaboratorioDTO.Correcao::acertou).count();
        return new ResultadoQuizLaboratorioDTO(acertos, perguntas.size(), acertos == perguntas.size(), 0, correcao);
    }

    /* ── Auxiliares do quiz ──────────────────────────────────────── */

    private static List<QuizLaboratorioDTO.Pergunta> perguntasSemGabarito(Laboratorio laboratorio) {
        return laboratorio.getPerguntas().stream()
                .map(p -> new QuizLaboratorioDTO.Pergunta(p.getId(), p.getEnunciado(),
                        p.getAlternativas().stream()
                                .map(a -> new QuizLaboratorioDTO.Alternativa(a.getId(), a.getTexto()))
                                .toList()))
                .toList();
    }

    /* Exatamente uma escolha por pergunta do laboratório, de uma
       alternativa dela; devolve a correção na ordem das perguntas. A tela
       nunca manda outra coisa — envio fora disso foi montado à mão e é
       recusado, em vez de a API escolher em silêncio qual valeria. */
    private static List<ResultadoQuizLaboratorioDTO.Correcao> corrigir(List<PerguntaLaboratorio> perguntas,
                                                                      RespostaQuizLaboratorioRequestDTO dto) {
        Map<Integer, Integer> escolhaPorPergunta = new HashMap<>();
        for (RespostaQuizLaboratorioRequestDTO.Escolha escolha : dto.escolhas()) {
            if (escolhaPorPergunta.putIfAbsent(escolha.perguntaId(), escolha.alternativaId()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Envie uma resposta só para cada pergunta.");
            }
        }
        boolean perguntaDeFora = escolhaPorPergunta.keySet().stream()
                .anyMatch(id -> perguntas.stream().noneMatch(p -> p.getId().equals(id)));
        if (perguntaDeFora) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "As perguntas mudaram. Abra o QR de novo para responder.");
        }

        List<ResultadoQuizLaboratorioDTO.Correcao> correcao = new ArrayList<>();
        for (PerguntaLaboratorio pergunta : perguntas) {
            Integer escolhida = escolhaPorPergunta.get(pergunta.getId());
            if (escolhida == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Responda todas as perguntas antes de enviar.");
            }
            boolean alternativaDaPergunta = pergunta.getAlternativas().stream()
                    .anyMatch(a -> a.getId().equals(escolhida));
            if (!alternativaDaPergunta) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "As perguntas mudaram. Abra o QR de novo para responder.");
            }
            Integer correta = pergunta.getAlternativas().stream()
                    .filter(a -> Boolean.TRUE.equals(a.getCorreta()))
                    .map(AlternativaPerguntaLaboratorio::getId)
                    .findFirst()
                    .orElse(null);
            correcao.add(new ResultadoQuizLaboratorioDTO.Correcao(pergunta.getId(), escolhida, escolhida.equals(correta)));
        }
        return correcao;
    }

    /* ── Comissão (/admin -> Quiz dos Laboratórios) ──────────────── */

    @Transactional(readOnly = true)
    public List<LaboratorioAdminDTO> listarParaAdmin(Integer ano) {
        List<Laboratorio> laboratorios = laboratorioRepository.findByAnoOrderByIdAsc(ano);
        Map<Integer, ContagemRespostasLaboratorioView> contagens = laboratorios.isEmpty() ? Map.of()
                : respostaRepository.contarPorLaboratorio(laboratorios.stream().map(Laboratorio::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(ContagemRespostasLaboratorioView::getLaboratorioId, Function.identity()));
        return laboratorios.stream()
                .map(l -> paraAdmin(l, contagens.get(l.getId())))
                .toList();
    }

    @Transactional
    public LaboratorioAdminDTO criar(LaboratorioRequestDTO dto) {
        Laboratorio laboratorio = new Laboratorio();
        laboratorio.setAno(dto.ano());
        laboratorio.setCodigoQr(gerarCodigoQrUnico());
        laboratorio.setQuizAberto(false);
        laboratorio.setCriadoEm(LocalDateTime.now());
        aplicar(laboratorio, dto);
        return paraAdmin(laboratorioRepository.save(laboratorio), null);
    }

    /* Troca os dados e a lista inteira de perguntas. O código do QR não
       muda: o QR já impresso continua valendo. */
    @Transactional
    public LaboratorioAdminDTO atualizar(Integer id, LaboratorioRequestDTO dto) {
        Laboratorio laboratorio = buscarOuFalhar(id);
        aplicar(laboratorio, dto);
        laboratorioRepository.saveAndFlush(laboratorio);
        return paraAdmin(laboratorio, contagemDe(id));
    }

    @Transactional
    public LaboratorioAdminDTO definirQuizAberto(Integer id, boolean aberto) {
        Laboratorio laboratorio = buscarOuFalhar(id);
        laboratorio.setQuizAberto(aberto);
        laboratorioRepository.save(laboratorio);
        return paraAdmin(laboratorio, contagemDe(id));
    }

    /* Remove o laboratório e, pelo ON DELETE CASCADE, as perguntas e as
       respostas. XP já creditado não é estornado. */
    @Transactional
    public void remover(Integer id) {
        laboratorioRepository.delete(buscarOuFalhar(id));
    }

    /* ── Auxiliares ──────────────────────────────────────────────── */

    private void aplicar(Laboratorio laboratorio, LaboratorioRequestDTO dto) {
        laboratorio.setNome(dto.nome().trim());
        String responsaveis = dto.responsaveis() == null ? "" : dto.responsaveis().trim();
        laboratorio.setResponsaveis(responsaveis.isEmpty() ? null : responsaveis);

        laboratorio.getPerguntas().clear();
        int ordemPergunta = 0;
        for (LaboratorioRequestDTO.Pergunta perguntaDto : dto.perguntas()) {
            ordemPergunta++;
            long corretas = perguntaDto.alternativas().stream().filter(LaboratorioRequestDTO.Alternativa::correta).count();
            if (corretas != 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A pergunta " + ordemPergunta + " precisa ter exatamente uma alternativa correta.");
            }
            PerguntaLaboratorio pergunta = new PerguntaLaboratorio();
            pergunta.setLaboratorio(laboratorio);
            pergunta.setOrdem(ordemPergunta);
            pergunta.setEnunciado(perguntaDto.enunciado().trim());

            int ordemAlternativa = 0;
            for (LaboratorioRequestDTO.Alternativa alternativaDto : perguntaDto.alternativas()) {
                AlternativaPerguntaLaboratorio alternativa = new AlternativaPerguntaLaboratorio();
                alternativa.setPergunta(pergunta);
                alternativa.setOrdem(++ordemAlternativa);
                alternativa.setTexto(alternativaDto.texto().trim());
                alternativa.setCorreta(alternativaDto.correta());
                pergunta.getAlternativas().add(alternativa);
            }
            laboratorio.getPerguntas().add(pergunta);
        }
    }

    private static ResultadoQuizLaboratorioDTO placar(RespostaQuizLaboratorio resposta) {
        return new ResultadoQuizLaboratorioDTO(resposta.getAcertos(), resposta.getTotal(),
                resposta.getTotal() > 0 && resposta.getAcertos().equals(resposta.getTotal()),
                resposta.getXpCreditado(), null);
    }

    private static ResponseStatusException jaRespondeu() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Você já respondeu o quiz deste laboratório.");
    }

    private ContagemRespostasLaboratorioView contagemDe(Integer id) {
        return respostaRepository.contarPorLaboratorio(List.of(id)).stream().findFirst().orElse(null);
    }

    private Laboratorio buscarPorCodigo(String codigoQr) {
        return laboratorioRepository.findByCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Este QR code não é de nenhum laboratório."));
    }

    private Laboratorio buscarOuFalhar(Integer id) {
        return laboratorioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Laboratório não encontrado."));
    }

    /* O token diz PARTICIPANTE, mas vale até expirar: confere no banco se
       a conta ainda é participante e está ativa antes de gravar e dar xp. */
    private Pessoa buscarParticipanteAtivo(Integer pessoaId) {
        Pessoa pessoa = pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
        if (pessoa.getRole() != Role.PARTICIPANTE || Boolean.FALSE.equals(pessoa.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Sua conta não pode responder o quiz agora. Procure a comissão.");
        }
        return pessoa;
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
        } while (laboratorioRepository.existsByCodigoQr(codigo));
        return codigo;
    }

    private LaboratorioAdminDTO paraAdmin(Laboratorio laboratorio, ContagemRespostasLaboratorioView contagem) {
        List<LaboratorioAdminDTO.Pergunta> perguntas = laboratorio.getPerguntas().stream()
                .map(p -> new LaboratorioAdminDTO.Pergunta(p.getId(), p.getEnunciado(),
                        p.getAlternativas().stream()
                                .map(a -> new LaboratorioAdminDTO.Alternativa(a.getId(), a.getTexto(),
                                        Boolean.TRUE.equals(a.getCorreta())))
                                .toList()))
                .toList();
        return new LaboratorioAdminDTO(
                laboratorio.getId(),
                laboratorio.getNome(),
                laboratorio.getResponsaveis(),
                laboratorio.getCodigoQr(),
                Boolean.TRUE.equals(laboratorio.getQuizAberto()),
                perguntas,
                contagem == null ? 0L : contagem.getRespostas(),
                contagem == null || contagem.getGabaritos() == null ? 0L : contagem.getGabaritos());
    }
}
