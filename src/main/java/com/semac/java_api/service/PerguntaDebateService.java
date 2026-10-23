package com.semac.java_api.service;

import com.semac.java_api.dto.DebatePerguntasAbertasDTO;
import com.semac.java_api.dto.PerguntaDebateEnviadaDTO;
import com.semac.java_api.dto.PerguntaDebateResponseDTO;
import com.semac.java_api.model.ConfiguracaoInscricao;
import com.semac.java_api.model.Evento;
import com.semac.java_api.model.PerguntaDebate;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.enums.CodigoTipoEvento;
import com.semac.java_api.repository.ConfiguracaoInscricaoRepository;
import com.semac.java_api.repository.EventoRepository;
import com.semac.java_api.repository.PerguntaDebateRepository;
import com.semac.java_api.repository.PessoaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/* Perguntas enviadas previamente para os debates, pela página pública
   /perguntas ou pelo /participantes. Qualquer pessoa envia, com ou sem
   login — professores e convidados não têm conta. A diretoria e a
   presidência leem no /admin, aba Conteúdo, e lá também incluem perguntas
   à mão (adicionarPelaComissao).

   Com login, a pergunta fica ligada à pessoa, vale o limite abaixo e a
   primeira pergunta de um participante dá a conquista PERGUNTA_DEBATE (ver
   ConquistaService.avaliarPerguntaDebate, que ignora a comissão). Sem
   login, o abuso é freado por IP (ver RateLimitConfig). */
@Service
public class PerguntaDebateService {

    /* Teto de perguntas por pessoa logada em um mesmo debate — folgado para
       uso real, mas impede que alguém encha a lista do /admin. */
    static final int LIMITE_PERGUNTAS_POR_PARTICIPANTE = 20;

    private final PerguntaDebateRepository perguntaDebateRepository;
    private final EventoRepository eventoRepository;
    private final PessoaRepository pessoaRepository;
    private final ConfiguracaoInscricaoRepository configuracaoInscricaoRepository;
    private final ConquistaService conquistaService;

    public PerguntaDebateService(PerguntaDebateRepository perguntaDebateRepository,
                                 EventoRepository eventoRepository,
                                 PessoaRepository pessoaRepository,
                                 ConfiguracaoInscricaoRepository configuracaoInscricaoRepository,
                                 ConquistaService conquistaService) {
        this.perguntaDebateRepository = perguntaDebateRepository;
        this.eventoRepository = eventoRepository;
        this.pessoaRepository = pessoaRepository;
        this.configuracaoInscricaoRepository = configuracaoInscricaoRepository;
        this.conquistaService = conquistaService;
    }

    /* `pessoaId` vem do token e é null sem login; aí vale `nomeAutor`
       (opcional). Devolve se esta pergunta destravou a conquista
       PERGUNTA_DEBATE agora. */
    @Transactional
    public PerguntaDebateEnviadaDTO enviar(Integer pessoaId, Integer eventoId, String texto, String nomeAutor) {
        Pessoa pessoa = pessoaId == null ? null : pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));

        Evento evento = buscarDebate(eventoId);
        exigirPerguntasAbertas(evento);

        if (pessoa != null && perguntaDebateRepository.countByEventoIdAndPessoaId(eventoId, pessoaId)
                >= LIMITE_PERGUNTAS_POR_PARTICIPANTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você já enviou o máximo de " + LIMITE_PERGUNTAS_POR_PARTICIPANTE
                            + " perguntas para este debate.");
        }

        PerguntaDebate pergunta = new PerguntaDebate();
        pergunta.setEvento(evento);
        pergunta.setPessoa(pessoa);
        pergunta.setNomeAutor(pessoa == null ? textoOuNulo(nomeAutor) : null);
        pergunta.setTexto(texto.trim());
        pergunta.setEnviadaEm(LocalDateTime.now());
        perguntaDebateRepository.save(pergunta);

        return new PerguntaDebateEnviadaDTO(conquistaService.avaliarPerguntaDebate(pessoa));
    }

    /* Debates que estão recebendo perguntas agora, para a página pública
       escolher: o interruptor do ano ligado e o debate ainda não terminado.
       Lista vazia quando nada está aberto. */
    @Transactional(readOnly = true)
    public List<DebatePerguntasAbertasDTO> listarDebatesAbertos() {
        LocalDateTime agora = LocalDateTime.now();
        return eventoRepository.buscarComTipoPorCodigos(List.of(CodigoTipoEvento.DEBATE)).stream()
                .filter(evento -> evento.getDataHoraFim() == null || evento.getDataHoraFim().isAfter(agora))
                .filter(this::perguntasAbertas)
                .sorted(Comparator.comparing(Evento::getDataHoraInicio))
                .map(evento -> new DebatePerguntasAbertasDTO(
                        evento.getId(), evento.getNome(), evento.getDataHoraInicio()))
                .toList();
    }

    /* Mais antigas primeiro: é a ordem em que a mediação costuma ler. */
    @Transactional(readOnly = true)
    public List<PerguntaDebateResponseDTO> listar(Integer eventoId) {
        buscarDebate(eventoId);
        return perguntaDebateRepository.findByEventoIdOrderByEnviadaEmAsc(eventoId).stream()
                .map(PerguntaDebateService::paraResposta)
                .toList();
    }

    /* Pergunta incluída à mão no /admin (ex.: entregue em papel no debate).
       Diferente do envio público: vale com as perguntas fechadas, sem o
       limite por pessoa e sem conquista — quem fez a pergunta é só o nome
       digitado; `adminId` (do token) fica registrado como quem cadastrou. */
    @Transactional
    public PerguntaDebateResponseDTO adicionarPelaComissao(Integer adminId, Integer eventoId,
                                                           String texto, String nomeAutor) {
        Pessoa admin = adminId == null ? null : pessoaRepository.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));

        PerguntaDebate pergunta = new PerguntaDebate();
        pergunta.setEvento(buscarDebate(eventoId));
        pergunta.setNomeAutor(textoOuNulo(nomeAutor));
        pergunta.setCadastradaPor(admin);
        pergunta.setTexto(texto.trim());
        pergunta.setEnviadaEm(LocalDateTime.now());
        return paraResposta(perguntaDebateRepository.save(pergunta));
    }

    /* Exclusão definitiva, pelo /admin. A conquista PERGUNTA_DEBATE de quem
       enviou fica: revogar seria uma surpresa para o participante. */
    @Transactional
    public void excluir(Integer perguntaId) {
        if (!perguntaDebateRepository.existsById(perguntaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pergunta não encontrada.");
        }
        perguntaDebateRepository.deleteById(perguntaId);
    }

    private static PerguntaDebateResponseDTO paraResposta(PerguntaDebate pergunta) {
        return new PerguntaDebateResponseDTO(
                pergunta.getId(),
                pergunta.getTexto(),
                nomeDoAutor(pergunta),
                pergunta.getPessoa() != null,
                pergunta.getCadastradaPor() != null ? pergunta.getCadastradaPor().getNome() : null,
                pergunta.getEnviadaEm());
    }

    private static String nomeDoAutor(PerguntaDebate pergunta) {
        if (pergunta.getPessoa() != null) return pergunta.getPessoa().getNome();
        return pergunta.getNomeAutor() != null ? pergunta.getNomeAutor() : "Anônimo";
    }

    private static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private Evento buscarDebate(Integer eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));
        if (evento.getTipoEvento() == null || evento.getTipoEvento().getCodigo() != CodigoTipoEvento.DEBATE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Perguntas só podem ser enviadas para debates.");
        }
        return evento;
    }

    /* Vale o ano do debate; ano sem configuração conta como fechado. */
    private boolean perguntasAbertas(Evento evento) {
        return configuracaoInscricaoRepository
                .findByAno(evento.getDataHoraInicio().getYear())
                .map(ConfiguracaoInscricao::getPerguntasDebateAbertas)
                .orElse(Boolean.FALSE);
    }

    private void exigirPerguntasAbertas(Evento evento) {
        if (!perguntasAbertas(evento)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O envio de perguntas para os debates está encerrado.");
        }
    }
}
