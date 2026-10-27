package com.semac.java_api.service;

import com.semac.java_api.dto.AvaliacaoEventoEnviadaDTO;
import com.semac.java_api.dto.MinhaAvaliacaoEventoDTO;
import com.semac.java_api.dto.ResumoAvaliacaoEventoDTO;
import com.semac.java_api.model.AvaliacaoEvento;
import com.semac.java_api.model.Evento;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.enums.CodigoTipoEvento;
import com.semac.java_api.model.enums.StatusPresenca;
import com.semac.java_api.model.pk.EventoParticipantePK;
import com.semac.java_api.repository.AvaliacaoEventoRepository;
import com.semac.java_api.repository.EventoParticipanteRepository;
import com.semac.java_api.repository.EventoRepository;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.repository.projection.InscritosEventoView;
import com.semac.java_api.repository.projection.ResumoAvaliacaoEventoView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/* Pesquisa de satisfação: o participante dá uma nota de 0 a 10 a cada
   evento da SEMAC em que esteve, pela área /participantes. A diretoria lê
   o resumo (média e quantidade por evento) no /admin, aba Conteúdo.

   Regras:
   - só os TIPOS_AVALIAVEIS — abertura, credenciamento, coffee break e
     encerramento ficam de fora;
   - só quem teve presença confirmada (PRESENTE) no evento;
   - só depois que o evento terminou;
   - a nota pode ser trocada: reenviar atualiza a mesma linha.

   Toda nota passa pela conquista AVALIACAO_COMPLETA (ver
   ConquistaService.avaliarAvaliacaoCompleta), que sai quando a pessoa
   avaliou tudo o que assistiu. */
@Service
public class AvaliacaoEventoService {

    /* Tipos que entram na pesquisa. Espelha TIPOS_FORA_AVALIACAO do
       frontend (pages/Participantes/data/apiEventosParticipantes.js) — lá
       o botão some, aqui a regra é garantida. Também é o denominador da
       conquista AVALIACAO_COMPLETA. */
    public static final Set<CodigoTipoEvento> TIPOS_AVALIAVEIS = EnumSet.complementOf(EnumSet.of(
            CodigoTipoEvento.ABERTURA,
            CodigoTipoEvento.CREDENCIAMENTO,
            CodigoTipoEvento.COFFEE_BREAK,
            CodigoTipoEvento.ENCERRAMENTO));

    private final AvaliacaoEventoRepository avaliacaoEventoRepository;
    private final EventoRepository eventoRepository;
    private final EventoParticipanteRepository eventoParticipanteRepository;
    private final PessoaRepository pessoaRepository;
    private final ConquistaService conquistaService;

    public AvaliacaoEventoService(AvaliacaoEventoRepository avaliacaoEventoRepository,
                                  EventoRepository eventoRepository,
                                  EventoParticipanteRepository eventoParticipanteRepository,
                                  PessoaRepository pessoaRepository,
                                  ConquistaService conquistaService) {
        this.avaliacaoEventoRepository = avaliacaoEventoRepository;
        this.eventoRepository = eventoRepository;
        this.eventoParticipanteRepository = eventoParticipanteRepository;
        this.pessoaRepository = pessoaRepository;
        this.conquistaService = conquistaService;
    }

    public static boolean ehAvaliavel(Evento evento) {
        return evento.getTipoEvento() != null && TIPOS_AVALIAVEIS.contains(evento.getTipoEvento().getCodigo());
    }

    /* Cria ou troca a nota do participante (`pessoaId` vem do token).
       Recusas de regra voltam como 409 com a mensagem para o modal. */
    @Transactional
    public AvaliacaoEventoEnviadaDTO avaliar(Integer pessoaId, Integer eventoId, int nota) {
        Pessoa pessoa = pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));

        if (!ehAvaliavel(evento)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este evento não faz parte da pesquisa de satisfação.");
        }
        if (evento.getDataHoraFim().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A avaliação abre quando o evento terminar.");
        }
        boolean compareceu = eventoParticipanteRepository.findById(new EventoParticipantePK(eventoId, pessoaId))
                .map(vinculo -> vinculo.getStatus() == StatusPresenca.PRESENTE)
                .orElse(false);
        if (!compareceu) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Só quem teve presença confirmada neste evento pode avaliá-lo.");
        }

        LocalDateTime agora = LocalDateTime.now();
        AvaliacaoEvento avaliacao = avaliacaoEventoRepository.findByEventoIdAndPessoaId(eventoId, pessoaId)
                .orElseGet(() -> {
                    AvaliacaoEvento nova = new AvaliacaoEvento();
                    nova.setEvento(evento);
                    nova.setPessoa(pessoa);
                    nova.setCriadaEm(agora);
                    return nova;
                });
        avaliacao.setNota(nota);
        avaliacao.setAtualizadaEm(agora);
        avaliacaoEventoRepository.save(avaliacao);

        boolean conquistaConcedida = conquistaService.avaliarAvaliacaoCompleta(pessoa);
        return new AvaliacaoEventoEnviadaDTO(eventoId, nota, conquistaConcedida);
    }

    @Transactional(readOnly = true)
    public List<MinhaAvaliacaoEventoDTO> listarDoParticipante(Integer pessoaId) {
        return avaliacaoEventoRepository.findByPessoaId(pessoaId).stream()
                .map(avaliacao -> new MinhaAvaliacaoEventoDTO(avaliacao.getEvento().getId(), avaliacao.getNota()))
                .toList();
    }

    /* Todos os eventos avaliáveis, em ordem de horário, inclusive os que
       ainda não receberam nota (o /admin mostra "—"). */
    @Transactional(readOnly = true)
    public List<ResumoAvaliacaoEventoDTO> resumir() {
        Map<Integer, ResumoAvaliacaoEventoView> notas = avaliacaoEventoRepository.resumirPorEvento().stream()
                .collect(Collectors.toMap(ResumoAvaliacaoEventoView::getEventoId, Function.identity()));
        Map<Integer, Long> presentes = eventoParticipanteRepository
                .contarInscritosPorEvento(List.of(StatusPresenca.PRESENTE)).stream()
                .collect(Collectors.toMap(InscritosEventoView::getEventoId, InscritosEventoView::getTotal));

        return eventoRepository.buscarComTipoPorCodigos(TIPOS_AVALIAVEIS).stream()
                .sorted(Comparator.comparing(Evento::getDataHoraInicio))
                .map(evento -> {
                    ResumoAvaliacaoEventoView resumo = notas.get(evento.getId());
                    return new ResumoAvaliacaoEventoDTO(
                            evento.getId(),
                            evento.getNome(),
                            evento.getTipoEvento().getNome(),
                            evento.getDataHoraInicio(),
                            evento.getDataHoraFim(),
                            presentes.getOrDefault(evento.getId(), 0L),
                            resumo == null ? 0 : resumo.getTotal(),
                            resumo == null ? null : resumo.getMedia());
                })
                .toList();
    }
}
