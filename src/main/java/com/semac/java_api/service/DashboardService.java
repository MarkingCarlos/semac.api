package com.semac.java_api.service;

import com.semac.java_api.dto.DashboardCheckinsDTO;
import com.semac.java_api.dto.DashboardDetalheCheckinDTO;
import com.semac.java_api.dto.DashboardEventoCheckinDTO;
import com.semac.java_api.dto.DashboardInscritoMinicursoDTO;
import com.semac.java_api.dto.DashboardLeituraQrDTO;
import com.semac.java_api.dto.DashboardLeiturasMembroDTO;
import com.semac.java_api.dto.DashboardMinicursoDTO;
import com.semac.java_api.dto.DashboardMinicursosDTO;
import com.semac.java_api.dto.DashboardRankingDTO;
import com.semac.java_api.model.Evento;
import com.semac.java_api.model.EventoParticipante;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.model.enums.StatusPresenca;
import com.semac.java_api.repository.EventoParticipanteRepository;
import com.semac.java_api.repository.EventoRepository;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.repository.projection.InscritosEventoView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/* Números da dashboard da seção Início do /admin. Só leitura; por
   enquanto a rota inteira é restrita à presidência (ver SecurityConfig).

   O volume é o de uma semana acadêmica (centenas de participantes,
   dezenas de eventos), então as agregações que não têm query própria são
   feitas em memória sobre listas pequenas. */
@Service
public class DashboardService {

    private static final Set<StatusPresenca> QUALQUER_STATUS = EnumSet.allOf(StatusPresenca.class);
    private static final Set<StatusPresenca> SO_PRESENTES = EnumSet.of(StatusPresenca.PRESENTE);

    /* Quem leu mais primeiro; o grupo "operador não registrado" (id nulo)
       sempre por último, para não disputar o topo com a comissão. */
    private static final Comparator<DashboardLeiturasMembroDTO> ORDEM_MEMBROS =
            Comparator.comparing((DashboardLeiturasMembroDTO m) -> m.operadorId() == null)
                    .thenComparing(DashboardLeiturasMembroDTO::leituras, Comparator.reverseOrder())
                    .thenComparing(m -> Objects.requireNonNullElse(m.operadorNome(), ""), String.CASE_INSENSITIVE_ORDER);

    private final EventoRepository eventoRepository;
    private final EventoParticipanteRepository eventoParticipanteRepository;
    private final PessoaRepository pessoaRepository;

    public DashboardService(EventoRepository eventoRepository,
                            EventoParticipanteRepository eventoParticipanteRepository,
                            PessoaRepository pessoaRepository) {
        this.eventoRepository = eventoRepository;
        this.eventoParticipanteRepository = eventoParticipanteRepository;
        this.pessoaRepository = pessoaRepository;
    }

    /* ── Vagas em minicursos ─────────────────────────────────────── */

    @Transactional(readOnly = true)
    public DashboardMinicursosDTO minicursos() {
        Map<Integer, Integer> ocupacao = contarPorEvento(InscricaoEventoService.STATUS_OCUPA_VAGA);

        List<DashboardMinicursoDTO> minicursos = eventoRepository
                .findByTipoEvento_ExigeInscricaoTrueOrderByDataHoraInicioAsc()
                .stream()
                .map(evento -> {
                    int capacidade = evento.getCapacidadeMaxima();
                    int inscritos = ocupacao.getOrDefault(evento.getId(), 0);
                    return new DashboardMinicursoDTO(evento.getId(), evento.getNome(),
                            evento.getDataHoraInicio(), evento.getLocal(), capacidade, inscritos,
                            Math.max(0, capacidade - inscritos));
                })
                .toList();

        int capacidadeTotal = minicursos.stream().mapToInt(DashboardMinicursoDTO::capacidade).sum();
        int inscritosTotal = minicursos.stream().mapToInt(DashboardMinicursoDTO::inscritos).sum();
        int vagasTotal = minicursos.stream().mapToInt(DashboardMinicursoDTO::vagasRestantes).sum();
        return new DashboardMinicursosDTO(capacidadeTotal, inscritosTotal, vagasTotal, minicursos);
    }

    @Transactional(readOnly = true)
    public List<DashboardInscritoMinicursoDTO> inscritosDoMinicurso(Integer eventoId) {
        Evento evento = buscarEvento(eventoId);
        if (!Boolean.TRUE.equals(evento.getTipoEvento().getExigeInscricao())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este evento não é um minicurso.");
        }
        return eventoParticipanteRepository
                .buscarComParticipantePorEvento(eventoId, InscricaoEventoService.STATUS_OCUPA_VAGA)
                .stream()
                .map(linha -> new DashboardInscritoMinicursoDTO(
                        linha.getParticipante().getId(),
                        linha.getParticipante().getNome(),
                        linha.getParticipante().getEmail(),
                        linha.getStatus() == StatusPresenca.PRESENTE))
                .sorted(Comparator.comparing(DashboardInscritoMinicursoDTO::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /* ── Ranking ─────────────────────────────────────────────────── */

    /* Todos os participantes confirmados, do maior xp para o menor. Empate
       divide a posição (1, 2, 2, 4), para não parecer que um está à frente
       do outro só pela ordem alfabética. */
    @Transactional(readOnly = true)
    public List<DashboardRankingDTO> ranking() {
        List<Pessoa> participantes = pessoaRepository.findByRoleAndXpIsNotNullOrderByXpDesc(Role.PARTICIPANTE)
                .stream()
                .sorted(Comparator.comparing(Pessoa::getXp).reversed()
                        .thenComparing(Pessoa::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<DashboardRankingDTO> ranking = new ArrayList<>();
        for (int i = 0; i < participantes.size(); i++) {
            Pessoa pessoa = participantes.get(i);
            int posicao = (i > 0 && pessoa.getXp().equals(participantes.get(i - 1).getXp()))
                    ? ranking.get(i - 1).posicao()
                    : i + 1;
            ranking.add(new DashboardRankingDTO(posicao, pessoa.getId(), pessoa.getNome(), pessoa.getEmail(),
                    pessoa.getXp(), pessoa.getNivel() == null ? null : pessoa.getNivel().getNome()));
        }
        return ranking;
    }

    /* ── Leitura de QR ───────────────────────────────────────────── */

    /* "Agora" é a mesma janela em que o /checkin aceita leitura: de 15min
       antes do início agendado até o fim do evento. */
    @Transactional(readOnly = true)
    public DashboardCheckinsDTO checkins() {
        LocalDateTime agora = LocalDateTime.now();
        Map<Integer, Integer> esperados = contarPorEvento(QUALQUER_STATUS);
        Map<Integer, Integer> leituras = contarPorEvento(SO_PRESENTES);

        List<DashboardEventoCheckinDTO> eventos = eventoRepository.findAll().stream()
                .filter(evento -> !aberturaDoCheckin(evento).isAfter(agora))
                .sorted(Comparator.comparing(Evento::getDataHoraInicio).reversed())
                .map(evento -> resumo(evento, esperados, leituras))
                .toList();

        List<DashboardEventoCheckinDTO> eventosAgora = eventos.stream()
                .filter(evento -> evento.dataHoraFim().isAfter(agora))
                .sorted(Comparator.comparing(DashboardEventoCheckinDTO::dataHoraInicio))
                .toList();

        List<DashboardLeiturasMembroDTO> porMembro = eventoParticipanteRepository.contarLeiturasPorOperador()
                .stream()
                .map(view -> new DashboardLeiturasMembroDTO(view.getOperadorId(), view.getOperadorNome(), view.getTotal()))
                .sorted(ORDEM_MEMBROS)
                .toList();

        long total = porMembro.stream().mapToLong(DashboardLeiturasMembroDTO::leituras).sum();
        return new DashboardCheckinsDTO(eventosAgora, eventos, porMembro, total);
    }

    @Transactional(readOnly = true)
    public DashboardDetalheCheckinDTO detalheCheckin(Integer eventoId) {
        Evento evento = buscarEvento(eventoId);
        List<EventoParticipante> linhas = eventoParticipanteRepository
                .buscarComParticipantePorEvento(eventoId, QUALQUER_STATUS);

        List<DashboardLeituraQrDTO> leituras = linhas.stream()
                .filter(linha -> linha.getStatus() == StatusPresenca.PRESENTE)
                .map(linha -> new DashboardLeituraQrDTO(
                        linha.getParticipante().getId(),
                        linha.getParticipante().getNome(),
                        linha.getRegistradoPorId(),
                        linha.getRegistradoPorNome(),
                        linha.getPresencaEm()))
                .sorted(Comparator.comparing(DashboardLeituraQrDTO::lidoEm,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        /* Agrupa por id (o nome é cópia do momento e pode variar se a
           pessoa foi renomeada); o nome exibido é o da leitura mais recente. */
        Map<Integer, List<DashboardLeituraQrDTO>> porOperador = leituras.stream()
                .collect(Collectors.groupingBy(l -> Objects.requireNonNullElse(l.operadorId(), -1),
                        LinkedHashMap::new, Collectors.toList()));
        List<DashboardLeiturasMembroDTO> porMembro = porOperador.values().stream()
                .map(lista -> new DashboardLeiturasMembroDTO(lista.get(0).operadorId(),
                        lista.get(0).operadorNome(), lista.size()))
                .sorted(ORDEM_MEMBROS)
                .toList();

        DashboardEventoCheckinDTO resumo = new DashboardEventoCheckinDTO(evento.getId(), evento.getNome(),
                evento.getTipoEvento().getNome(), evento.getDataHoraInicio(), evento.getDataHoraFim(),
                linhas.size(), leituras.size());
        return new DashboardDetalheCheckinDTO(resumo, porMembro, leituras);
    }

    /* ── Auxiliares ──────────────────────────────────────────────── */

    private LocalDateTime aberturaDoCheckin(Evento evento) {
        return evento.getDataHoraInicio().minusMinutes(InscricaoEventoService.ANTECEDENCIA_MAXIMA_CHECKIN_MINUTOS);
    }

    private DashboardEventoCheckinDTO resumo(Evento evento, Map<Integer, Integer> esperados,
                                            Map<Integer, Integer> leituras) {
        return new DashboardEventoCheckinDTO(evento.getId(), evento.getNome(), evento.getTipoEvento().getNome(),
                evento.getDataHoraInicio(), evento.getDataHoraFim(),
                esperados.getOrDefault(evento.getId(), 0), leituras.getOrDefault(evento.getId(), 0));
    }

    private Map<Integer, Integer> contarPorEvento(Set<StatusPresenca> status) {
        return eventoParticipanteRepository.contarInscritosPorEvento(status).stream()
                .collect(Collectors.toMap(InscritosEventoView::getEventoId, view -> view.getTotal().intValue()));
    }

    private Evento buscarEvento(Integer eventoId) {
        return eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));
    }
}
