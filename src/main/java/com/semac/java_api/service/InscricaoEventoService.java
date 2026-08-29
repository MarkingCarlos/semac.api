package com.semac.java_api.service;

import com.semac.java_api.dto.PresencaConfirmadaDTO;
import com.semac.java_api.model.Evento;
import com.semac.java_api.model.EventoParticipante;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.model.enums.StatusPresenca;
import com.semac.java_api.model.pk.EventoParticipantePK;
import com.semac.java_api.repository.EventoParticipanteRepository;
import com.semac.java_api.repository.EventoRepository;
import com.semac.java_api.repository.NivelRepository;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.repository.projection.InscritosEventoView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/* Regras de `evento_participante` — quem está em qual evento.

   Dois fluxos distintos:

   - Eventos abertos (`tipo_evento.exige_inscricao = false`: palestra,
     mesa redonda, debate): todo participante confirmado é pré-inscrito
     automaticamente, na confirmação da inscrição e na criação do evento.
     A capacidade é folgada de propósito e não é checada.

   - Minicursos (`exige_inscricao = true`): o participante escolhe na
     área /participantes. Vagas são limitadas, então a inscrição valida
     lotação (sob lock da linha do evento) e choque de horário — a regra
     é um minicurso por faixa de horário. */
@Service
public class InscricaoEventoService {

    /* Status que ocupam vaga. AUSENTE fica de fora: ele é atribuído
       quando o evento termina sem check-in, e aí a vaga não existe mais. */
    private static final Set<StatusPresenca> STATUS_OCUPA_VAGA =
            Set.of(StatusPresenca.INSCRITO, StatusPresenca.PRESENTE);

    /* Regra de atraso no check-in (ver marcarPresente): abaixo de
       ATRASO_METADE_XP_MINUTOS credita o xp cheio do tipo de evento; a
       partir daí e até ATRASO_ZERO_XP_MINUTOS (exclusive), metade; a
       partir de ATRASO_ZERO_XP_MINUTOS, a presença é registrada mas sem
       xp. Hardcoded por ora — mesmo padrão usado nas regras de conquista. */
    private static final long ATRASO_METADE_XP_MINUTOS = 20;
    private static final long ATRASO_ZERO_XP_MINUTOS = 30;

    private final EventoRepository eventoRepository;
    private final EventoParticipanteRepository eventoParticipanteRepository;
    private final PessoaRepository pessoaRepository;
    private final NivelRepository nivelRepository;

    public InscricaoEventoService(EventoRepository eventoRepository,
                                  EventoParticipanteRepository eventoParticipanteRepository,
                                  PessoaRepository pessoaRepository,
                                  NivelRepository nivelRepository) {
        this.eventoRepository = eventoRepository;
        this.eventoParticipanteRepository = eventoParticipanteRepository;
        this.pessoaRepository = pessoaRepository;
        this.nivelRepository = nivelRepository;
    }

    /* ── Ocupação (usada para calcular vagas restantes) ──────────── */

    /* eventoId → vagas ocupadas, em uma query só (evita N+1 na listagem). */
    @Transactional(readOnly = true)
    public Map<Integer, Long> ocupacaoPorEvento() {
        return eventoParticipanteRepository.contarInscritosPorEvento(STATUS_OCUPA_VAGA).stream()
                .collect(Collectors.toMap(InscritosEventoView::getEventoId, InscritosEventoView::getTotal));
    }

    @Transactional(readOnly = true)
    public long ocupacaoDoEvento(Integer eventoId) {
        return eventoParticipanteRepository.countByPk_EventoIdAndStatusIn(eventoId, STATUS_OCUPA_VAGA);
    }

    /* ── Pré-inscrição automática nos eventos abertos ────────────── */

    /* Confirmação da inscrição (role = PARTICIPANTE): entra em todos os
       eventos abertos que já existem. Idempotente — pode rodar de novo
       sem duplicar. */
    @Transactional
    public void preInscreverEmEventosAbertos(Pessoa participante) {
        List<Evento> abertos = eventoRepository.findByTipoEvento_ExigeInscricaoFalse();
        for (Evento evento : abertos) {
            inserirSeAusente(evento.getId(), participante.getId());
        }
    }

    /* Evento aberto recém-criado (ou que deixou de ser minicurso): entra
       para todo participante já confirmado. */
    @Transactional
    public void preInscreverParticipantesNoEvento(Evento evento) {
        for (Pessoa participante : pessoaRepository.findAllByRole(Role.PARTICIPANTE)) {
            inserirSeAusente(evento.getId(), participante.getId());
        }
    }

    /* Evento que passou a exigir inscrição: as pré-inscrições automáticas
       precisam sair, senão o minicurso nasce lotado com a base inteira.
       Presenças já registradas são preservadas. */
    @Transactional
    public void removerPreInscricoesDoEvento(Evento evento) {
        List<EventoParticipante> inscritos =
                eventoParticipanteRepository.findByPk_EventoIdAndStatus(evento.getId(), StatusPresenca.INSCRITO);
        eventoParticipanteRepository.deleteAll(inscritos);
    }

    /* Evento excluído leva junto a lista de quem estava nele — sem isso a
       FK de evento_participante barra a exclusão no /admin. */
    @Transactional
    public void removerInscricoesDoEvento(Integer eventoId) {
        eventoParticipanteRepository.deleteByPk_EventoId(eventoId);
    }

    /* Pessoa que virou comissão deixa de ser participante do evento
       (organizador não pontua nem aparece em evento_participante). */
    @Transactional
    public void removerInscricoesDoParticipante(Integer participanteId) {
        eventoParticipanteRepository.deleteByPk_ParticipanteId(participanteId);
    }

    /* ── Escolha de minicurso pelo participante ──────────────────── */

    @Transactional(readOnly = true)
    public List<EventoParticipante> listarInscricoesDoParticipante(Integer participanteId) {
        return eventoParticipanteRepository.buscarComEventoPorParticipante(participanteId);
    }

    @Transactional
    public void inscrever(Integer participanteId, Integer eventoId) {
        exigirParticipante(participanteId);

        /* Lock pessimista: a contagem de vagas e a gravação precisam
           acontecer sem outra inscrição no meio, senão duas pessoas
           pegam a última vaga. */
        Evento evento = eventoRepository.buscarParaInscricao(eventoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));

        if (!exigeInscricao(evento)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse evento é aberto: todo participante confirmado já está na lista.");
        }
        if (evento.getDataHoraInicio().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse minicurso já começou.");
        }

        EventoParticipantePK pk = new EventoParticipantePK(eventoId, participanteId);
        if (eventoParticipanteRepository.existsById(pk)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já está inscrito nesse minicurso.");
        }

        if (ocupacaoDoEvento(eventoId) >= evento.getCapacidadeMaxima()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse minicurso está esgotado.");
        }

        Evento conflito = minicursoNoMesmoHorario(participanteId, evento);
        if (conflito != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você já escolheu \"" + conflito.getNome() + "\" nesse horário.");
        }

        EventoParticipante inscricao = new EventoParticipante();
        inscricao.setPk(pk);
        inscricao.setEvento(evento);
        inscricao.setParticipante(pessoaRepository.getReferenceById(participanteId));
        inscricao.setStatus(StatusPresenca.INSCRITO);
        eventoParticipanteRepository.save(inscricao);
    }

    @Transactional
    public void cancelar(Integer participanteId, Integer eventoId) {
        exigirParticipante(participanteId);

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));

        if (!exigeInscricao(evento)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Eventos abertos não podem ser cancelados.");
        }

        EventoParticipante inscricao = eventoParticipanteRepository
                .findById(new EventoParticipantePK(eventoId, participanteId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Você não está inscrito nesse minicurso."));

        if (inscricao.getStatus() == StatusPresenca.PRESENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Sua presença nesse minicurso já foi registrada.");
        }
        if (evento.getDataHoraInicio().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse minicurso já começou.");
        }

        eventoParticipanteRepository.delete(inscricao);
    }

    /* ── Check-in por QR code (ferramenta /checkin) ──────────────── */

    /* Marca presença a partir do uuid do crachá do participante (leitura
       de câmera). "Não cadastrado" cobre tanto uuid inexistente quanto
       pessoa sem inscrição nesse evento específico — para quem opera o
       check-in os dois casos pedem a mesma ação (busca manual ou
       confirmar a inscrição na secretaria), então a mensagem é a mesma. */
    @Transactional
    public PresencaConfirmadaDTO registrarPresencaPorUuid(Integer eventoId, String uuid) {
        Pessoa participante = pessoaRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Este participante não está cadastrado para esse evento."));
        return marcarPresente(eventoId, participante);
    }

    /* Confirmação manual (busca por nome/e-mail na ferramenta /checkin,
       quando a leitura do QR falha). O participante já foi identificado
       visualmente pela lista, então basta o id. */
    @Transactional
    public PresencaConfirmadaDTO registrarPresencaPorId(Integer eventoId, Integer participanteId) {
        Pessoa participante = pessoaRepository.findById(participanteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Este participante não está cadastrado para esse evento."));
        return marcarPresente(eventoId, participante);
    }

    private PresencaConfirmadaDTO marcarPresente(Integer eventoId, Pessoa participante) {
        EventoParticipante inscricao = eventoParticipanteRepository
                .findById(new EventoParticipantePK(eventoId, participante.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Este participante não está cadastrado para esse evento."));

        if (inscricao.getStatus() == StatusPresenca.PRESENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Presença já registrada para " + participante.getNome() + " nesta palestra.");
        }

        LocalDateTime agora = LocalDateTime.now();
        Evento evento = inscricao.getEvento();
        long atrasoMinutos = Math.max(0,
                Duration.between(evento.getDataHoraInicio(), agora).toMinutes());
        int xpCreditado = calcularXpCreditado(evento, atrasoMinutos);

        inscricao.setStatus(StatusPresenca.PRESENTE);
        inscricao.setPresencaEm(agora);
        inscricao.setXpCreditado(xpCreditado);
        eventoParticipanteRepository.save(inscricao);

        if (xpCreditado > 0) {
            creditarXp(participante, xpCreditado);
        }

        String infoAdicional = participante.getTipoInscricao() != null
                ? participante.getTipoInscricao().getNome()
                : participante.getEmail();
        return new PresencaConfirmadaDTO(participante.getNome(), infoAdicional, xpCreditado, atrasoMinutos);
    }

    /* Xp cheio do tipo de evento; metade a partir de ATRASO_METADE_XP_MINUTOS
       (arredondado pra baixo); zero a partir de ATRASO_ZERO_XP_MINUTOS —
       a presença continua registrada, só o xp que muda. */
    private int calcularXpCreditado(Evento evento, long atrasoMinutos) {
        int pontosBase = evento.getTipoEvento().getPontos();
        if (atrasoMinutos >= ATRASO_ZERO_XP_MINUTOS) {
            return 0;
        }
        if (atrasoMinutos >= ATRASO_METADE_XP_MINUTOS) {
            return pontosBase / 2;
        }
        return pontosBase;
    }

    /* Soma o xp na pessoa e recalcula o nível correspondente, mesmo
       critério usado na confirmação da inscrição (ver
       PessoaService.atribuirRole). */
    private void creditarXp(Pessoa participante, int xpCreditado) {
        int xpAtual = participante.getXp() == null ? 0 : participante.getXp();
        int novoXp = xpAtual + xpCreditado;
        participante.setXp(novoXp);
        nivelRepository.findTopByXpMinimoLessThanEqualOrderByXpMinimoDesc(novoXp)
                .ifPresent(participante::setNivel);
        pessoaRepository.save(participante);
    }

    /* ── Auxiliares ─────────────────────────────────────────────── */

    /* Um minicurso por faixa de horário: devolve o minicurso já escolhido
       que se sobrepõe ao candidato, ou null se o horário está livre.
       Intervalos são [início, fim) — terminar 14h e começar 14h não é
       conflito. */
    private Evento minicursoNoMesmoHorario(Integer participanteId, Evento candidato) {
        return listarInscricoesDoParticipante(participanteId).stream()
                .map(EventoParticipante::getEvento)
                .filter(this::exigeInscricao)
                .filter(escolhido -> !escolhido.getId().equals(candidato.getId()))
                .filter(escolhido -> candidato.getDataHoraInicio().isBefore(escolhido.getDataHoraFim())
                        && escolhido.getDataHoraInicio().isBefore(candidato.getDataHoraFim()))
                .findFirst()
                .orElse(null);
    }

    private boolean exigeInscricao(Evento evento) {
        return evento.getTipoEvento() != null
                && Boolean.TRUE.equals(evento.getTipoEvento().getExigeInscricao());
    }

    private void exigirParticipante(Integer participanteId) {
        Pessoa pessoa = pessoaRepository.findById(participanteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
        if (pessoa.getRole() != Role.PARTICIPANTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas participantes confirmados escolhem minicursos.");
        }
    }

    private void inserirSeAusente(Integer eventoId, Integer participanteId) {
        EventoParticipantePK pk = new EventoParticipantePK(eventoId, participanteId);
        if (eventoParticipanteRepository.existsById(pk)) {
            return;
        }
        EventoParticipante inscricao = new EventoParticipante();
        inscricao.setPk(pk);
        inscricao.setEvento(eventoRepository.getReferenceById(eventoId));
        inscricao.setParticipante(pessoaRepository.getReferenceById(participanteId));
        inscricao.setStatus(StatusPresenca.INSCRITO);
        eventoParticipanteRepository.save(inscricao);
    }
}
