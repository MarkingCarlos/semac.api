package com.semac.java_api.service;

import com.semac.java_api.config.CatalogoVariaveisEmail;
import com.semac.java_api.config.CatalogoVariaveisEmail.VariavelEmail;
import com.semac.java_api.dto.ComunicadoRequestDTO;
import com.semac.java_api.dto.ComunicadoResponseDTO;
import com.semac.java_api.dto.DestinatariosResponseDTO;
import com.semac.java_api.dto.PublicoComunicadoDTO;
import com.semac.java_api.event.ComunicadoDisparadoEvent;
import com.semac.java_api.event.ComunicadoDisparadoEvent.Destinatario;
import com.semac.java_api.model.Comunicado;
import com.semac.java_api.model.Evento;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.enums.PublicoComunicado;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.model.enums.StatusComunicado;
import com.semac.java_api.repository.ComunicadoRepository;
import com.semac.java_api.repository.EventoParticipanteRepository;
import com.semac.java_api.repository.EventoRepository;
import com.semac.java_api.repository.PessoaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/* Comunicados avulsos: mensagem escrita na hora e disparada para um
   público escolhido em /admin -> Comunicados.

   Três cuidados que moldam o desenho daqui:

     - o envio é individual, não BCC: permite personalizar com
       {{nomeParticipante}} e não expõe o e-mail de um participante para
       os outros;

     - roda em lote assíncrono com intervalo entre mensagens, porque 400
       envios levam minutos e o limite diário do Gmail é ~500;

     - cada disparo vira uma linha em `comunicado`. E-mail não tem
       desfazer: o histórico é o que permite saber depois o que saiu. */
@Service
public class ComunicadoService {

    private static final Logger log = LoggerFactory.getLogger(ComunicadoService.class);

    private static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    /* Amostra mostrada na confirmação: o suficiente para a pessoa
       reconhecer o público sem despejar 400 e-mails na tela. */
    private static final int TAMANHO_AMOSTRA = 5;

    private static final int LIMITE_HISTORICO = 30;

    private final ComunicadoRepository comunicadoRepository;
    private final PessoaRepository pessoaRepository;
    private final EventoRepository eventoRepository;
    private final EventoParticipanteRepository eventoParticipanteRepository;
    private final RenderizadorEmailService renderizador;
    private final EmailService emailService;
    private final ApplicationEventPublisher publicadorEventos;
    private final String urlSite;
    private final long intervaloLoteMs;

    public ComunicadoService(ComunicadoRepository comunicadoRepository,
                             PessoaRepository pessoaRepository,
                             EventoRepository eventoRepository,
                             EventoParticipanteRepository eventoParticipanteRepository,
                             RenderizadorEmailService renderizador,
                             EmailService emailService,
                             ApplicationEventPublisher publicadorEventos,
                             @Value("${app.site.url}") String urlSite,
                             @Value("${app.mail.intervalo.lote.ms}") long intervaloLoteMs) {
        this.comunicadoRepository = comunicadoRepository;
        this.pessoaRepository = pessoaRepository;
        this.eventoRepository = eventoRepository;
        this.eventoParticipanteRepository = eventoParticipanteRepository;
        this.renderizador = renderizador;
        this.emailService = emailService;
        this.publicadorEventos = publicadorEventos;
        this.urlSite = urlSite;
        this.intervaloLoteMs = intervaloLoteMs;
    }

    @Transactional(readOnly = true)
    public List<PublicoComunicadoDTO> publicos() {
        return java.util.Arrays.stream(PublicoComunicado.values())
                .map(publico -> new PublicoComunicadoDTO(
                        publico.name(),
                        publico.getRotulo(),
                        publico.isExigeEvento(),
                        /* Público de evento depende de qual evento, então a
                           contagem só existe depois da escolha. */
                        publico.isExigeEvento() ? null : resolverDestinatarios(publico, null).size()))
                .toList();
    }

    /* Alimenta a tela de confirmação: quantos vão receber e alguns nomes.
       Ninguém dispara para "todos" sem ver o número antes. */
    @Transactional(readOnly = true)
    public DestinatariosResponseDTO destinatarios(PublicoComunicado publico, Integer eventoId) {
        List<Destinatario> lista = resolverDestinatarios(publico, eventoId);
        return new DestinatariosResponseDTO(
                lista.size(),
                lista.stream().limit(TAMANHO_AMOSTRA).map(Destinatario::nome).toList());
    }

    public String previa(String corpoMarkdown) {
        validarVariaveis(corpoMarkdown);
        Map<String, String> exemplos = new LinkedHashMap<>();
        for (VariavelEmail variavel : CatalogoVariaveisEmail.VARIAVEIS_COMUNICADO) {
            exemplos.put(variavel.nome(), variavel.exemplo());
        }
        return renderizador.renderizar(corpoMarkdown, exemplos);
    }

    public void enviarTeste(Pessoa destinatario, String assunto, String corpoMarkdown) {
        validarVariaveis(corpoMarkdown);
        emailService.enviarHtmlPronto(destinatario.getEmail(), "[TESTE] " + assunto,
                renderizador.renderizar(corpoMarkdown, variaveisDe(destinatario.getNome())));
    }

    /* Registra o disparo e devolve na hora. O envio começa depois do
       commit (ComunicadoEnvioListener) e leva minutos — quem clicou não
       pode ficar preso esperando. */
    @Transactional
    public ComunicadoResponseDTO disparar(ComunicadoRequestDTO dto, Integer idAutor) {
        validarVariaveis(dto.corpoMarkdown());

        PublicoComunicado publico = PublicoComunicado.deTexto(dto.publico());
        Evento evento = resolverEvento(publico, dto.eventoId());

        List<Destinatario> destinatarios = resolverDestinatarios(publico, dto.eventoId());
        if (destinatarios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nenhum destinatário neste público — nada foi enviado.");
        }

        Comunicado comunicado = new Comunicado();
        comunicado.setAssunto(dto.assunto().trim());
        comunicado.setCorpoMarkdown(dto.corpoMarkdown());
        comunicado.setPublico(publico);
        comunicado.setEvento(evento);
        comunicado.setTotalDestinatarios(destinatarios.size());
        comunicado.setStatus(StatusComunicado.EM_ANDAMENTO);
        comunicado.setCriadoEm(LocalDateTime.now());
        pessoaRepository.findById(idAutor).ifPresent(comunicado::setEnviadoPor);

        Comunicado salvo = comunicadoRepository.save(comunicado);

        publicadorEventos.publishEvent(new ComunicadoDisparadoEvent(
                salvo.getId(), salvo.getAssunto(), salvo.getCorpoMarkdown(), destinatarios));

        return paraResposta(salvo);
    }

    /* O lote em si. Roda no executorComunicado, separado do pool dos
       e-mails transacionais para que um disparo longo não segure a
       confirmação de inscrição de quem acabou de ser confirmado. */
    @Async("executorComunicado")
    public void processarLote(ComunicadoDisparadoEvent evento) {
        int enviados = 0;
        int falhas = 0;
        int ignorados = 0;

        log.info("Comunicado {}: iniciando envio para {} destinatário(s).",
                evento.comunicadoId(), evento.destinatarios().size());

        for (Destinatario destinatario : evento.destinatarios()) {
            String html = renderizador.renderizar(evento.corpoMarkdown(), variaveisDe(destinatario.nome()));
            switch (emailService.enviarAgora(destinatario.email(), evento.assunto(), html)) {
                case ENVIADO -> enviados++;
                case FALHOU -> falhas++;
                /* Envio desligado: não conta como falha, senão o histórico
                   acusaria um problema inexistente. */
                case IGNORADO -> ignorados++;
            }
            if (!pausar()) {
                log.warn("Comunicado {}: envio interrompido no desligamento da aplicação.", evento.comunicadoId());
                break;
            }
        }

        if (ignorados > 0) {
            log.info("Comunicado {}: {} mensagem(ns) não saíram por envio desligado.",
                    evento.comunicadoId(), ignorados);
        }
        concluir(evento.comunicadoId(), enviados, falhas);
    }

    /* Sem @Transactional de propósito: roda fora da transação original, e
       o save do repositório já abre a sua. */
    private void concluir(Integer comunicadoId, int enviados, int falhas) {
        comunicadoRepository.findById(comunicadoId).ifPresent(comunicado -> {
            comunicado.setEnviados(enviados);
            comunicado.setFalhas(falhas);
            comunicado.setConcluidoEm(LocalDateTime.now());
            comunicado.setStatus(falhas > 0 ? StatusComunicado.CONCLUIDO_COM_FALHAS : StatusComunicado.CONCLUIDO);
            comunicadoRepository.save(comunicado);
        });
        log.info("Comunicado {}: concluído — {} enviado(s), {} falha(s).", comunicadoId, enviados, falhas);
    }

    /* false quando a thread foi interrompida (desligamento da aplicação). */
    private boolean pausar() {
        try {
            Thread.sleep(intervaloLoteMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<ComunicadoResponseDTO> historico() {
        return comunicadoRepository.findAllByOrderByCriadoEmDesc(Limit.of(LIMITE_HISTORICO)).stream()
                .map(this::paraResposta)
                .toList();
    }

    /* Toda opção exclui pessoa desativada e sem e-mail: ninguém suspenso
       recebe comunicado, e endereço vazio só geraria falha no lote. */
    private List<Destinatario> resolverDestinatarios(PublicoComunicado publico, Integer eventoId) {
        List<Pessoa> pessoas = switch (publico) {
            case PARTICIPANTES_CONFIRMADOS -> pessoaRepository.findAllByRole(Role.PARTICIPANTE);
            case INSCRICOES_PENDENTES -> pessoaRepository.findAllByRoleIsNull();
            case COMISSAO -> pessoaRepository.findAllByRoleNot(Role.PARTICIPANTE);
            case INSCRITOS_EM_EVENTO -> eventoParticipanteRepository.findByPk_EventoId(exigirEventoId(eventoId))
                    .stream()
                    .map(inscricao -> inscricao.getParticipante())
                    .toList();
        };

        return pessoas.stream()
                .filter(p -> p != null && Boolean.TRUE.equals(p.getAtivo()))
                .filter(p -> p.getEmail() != null && !p.getEmail().isBlank())
                /* distinct por e-mail: a mesma pessoa não pode receber duas
                   vezes o mesmo comunicado. */
                .collect(Collectors.toMap(Pessoa::getEmail, p -> p, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(Pessoa::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(p -> new Destinatario(p.getNome(), p.getEmail()))
                .toList();
    }

    private Map<String, String> variaveisDe(String nome) {
        Map<String, String> variaveis = new LinkedHashMap<>();
        variaveis.put("nomeParticipante", nome == null ? "" : nome);
        variaveis.put("urlAreaParticipante", urlSite + "/participantes");
        return variaveis;
    }

    private void validarVariaveis(String corpoMarkdown) {
        renderizador.validarVariaveis(corpoMarkdown, CatalogoVariaveisEmail.VARIAVEIS_COMUNICADO.stream()
                .map(VariavelEmail::nome)
                .collect(Collectors.toSet()));
    }

    private Evento resolverEvento(PublicoComunicado publico, Integer eventoId) {
        if (!publico.isExigeEvento()) {
            return null;
        }
        return eventoRepository.findById(exigirEventoId(eventoId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evento inválido."));
    }

    private Integer exigirEventoId(Integer eventoId) {
        if (eventoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Escolha o evento para este público.");
        }
        return eventoId;
    }

    private ComunicadoResponseDTO paraResposta(Comunicado comunicado) {
        Pessoa autor = comunicado.getEnviadoPor();
        Evento evento = comunicado.getEvento();

        return new ComunicadoResponseDTO(
                comunicado.getId(),
                comunicado.getAssunto(),
                comunicado.getPublico().name(),
                comunicado.getPublico().getRotulo(),
                evento == null ? null : evento.getNome(),
                comunicado.getTotalDestinatarios(),
                comunicado.getEnviados(),
                comunicado.getFalhas(),
                comunicado.getStatus().name(),
                comunicado.getCriadoEm().format(FORMATO_DATA_HORA),
                autor == null ? null : autor.getNome());
    }
}
