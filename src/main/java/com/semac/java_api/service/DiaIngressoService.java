package com.semac.java_api.service;

import com.semac.java_api.dto.DiasIngressoResponseDTO;
import com.semac.java_api.model.Evento;
import com.semac.java_api.model.EventoParticipante;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.PessoaDiaIngresso;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.model.enums.StatusPresenca;
import com.semac.java_api.repository.EventoParticipanteRepository;
import com.semac.java_api.repository.EventoRepository;
import com.semac.java_api.repository.PessoaDiaIngressoRepository;
import com.semac.java_api.repository.PessoaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/* Dias em que vale o ingresso diário (tipo_inscricao.por_dia).

   Quem comprou N diárias escolhe N dias em /participantes, entre os dias
   que têm algum evento na programação. O QR do diarista só passa no
   check-in desses dias (ver InscricaoEventoService.exigirDiaDoIngresso)
   e ele só escolhe minicurso neles.

   A escolha pode ser trocada até o dia começar: um dia escolhido que já é
   hoje (ou passou) fica travado, senão daria pra usar a diária e depois
   "devolvê-la" trocando por outro dia. Pelo mesmo motivo não se escolhe um
   dia que já passou. */
@Service
public class DiaIngressoService {

    private static final String[] ROTULOS_DIA_SEMANA = {"SEG", "TER", "QUA", "QUI", "SEX", "SÁB", "DOM"};
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private final PessoaRepository pessoaRepository;
    private final EventoRepository eventoRepository;
    private final EventoParticipanteRepository eventoParticipanteRepository;
    private final PessoaDiaIngressoRepository pessoaDiaIngressoRepository;

    public DiaIngressoService(PessoaRepository pessoaRepository,
                              EventoRepository eventoRepository,
                              EventoParticipanteRepository eventoParticipanteRepository,
                              PessoaDiaIngressoRepository pessoaDiaIngressoRepository) {
        this.pessoaRepository = pessoaRepository;
        this.eventoRepository = eventoRepository;
        this.eventoParticipanteRepository = eventoParticipanteRepository;
        this.pessoaDiaIngressoRepository = pessoaDiaIngressoRepository;
    }

    @Transactional(readOnly = true)
    public DiasIngressoResponseDTO buscar(Integer pessoaId) {
        Pessoa pessoa = buscarPessoa(pessoaId);
        if (!ehDiarista(pessoa)) {
            return new DiasIngressoResponseDTO(false, 0, List.of(), List.of(), List.of());
        }

        List<LocalDate> escolhidos = diasEscolhidos(pessoaId);
        LocalDate hoje = LocalDate.now();
        List<LocalDate> travados = escolhidos.stream().filter(dia -> !dia.isAfter(hoje)).toList();

        return new DiasIngressoResponseDTO(true, diasContratados(pessoa),
                List.copyOf(diasDisponiveis()), escolhidos, travados);
    }

    /* Substitui a escolha inteira. Minicursos em dias que saíram da
       escolha são desfeitos, liberando a vaga — o diarista não entraria
       neles de qualquer forma. */
    @Transactional
    public DiasIngressoResponseDTO salvar(Integer pessoaId, List<LocalDate> diasPedidos) {
        Pessoa pessoa = buscarPessoa(pessoaId);
        if (pessoa.getRole() != Role.PARTICIPANTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas participantes confirmados escolhem os dias do ingresso.");
        }
        if (!ehDiarista(pessoa)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Seu ingresso vale para todos os dias do evento.");
        }

        Set<LocalDate> novos = new TreeSet<>(diasPedidos);
        int contratados = diasContratados(pessoa);
        if (novos.size() != contratados) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Escolha exatamente " + contratados + (contratados == 1 ? " dia." : " dias."));
        }

        Set<LocalDate> disponiveis = diasDisponiveis();
        LocalDate hoje = LocalDate.now();
        List<LocalDate> atuais = diasEscolhidos(pessoaId);

        for (LocalDate dia : novos) {
            if (!disponiveis.contains(dia)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        formatarDia(dia) + " não tem programação no evento.");
            }
            if (dia.isBefore(hoje) && !atuais.contains(dia)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        formatarDia(dia) + " já passou.");
            }
        }
        for (LocalDate dia : atuais) {
            if (!dia.isAfter(hoje) && !novos.contains(dia)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        formatarDia(dia) + " já começou e não pode mais ser trocado.");
            }
        }

        List<PessoaDiaIngresso> registros = pessoaDiaIngressoRepository.findByPessoaIdOrderByDiaAsc(pessoaId);
        Set<LocalDate> removidos = registros.stream()
                .map(PessoaDiaIngresso::getDia)
                .filter(dia -> !novos.contains(dia))
                .collect(Collectors.toSet());

        pessoaDiaIngressoRepository.deleteAll(registros.stream()
                .filter(registro -> removidos.contains(registro.getDia()))
                .toList());
        for (LocalDate dia : novos) {
            if (!atuais.contains(dia)) {
                pessoaDiaIngressoRepository.save(new PessoaDiaIngresso(null, pessoaId, dia));
            }
        }

        if (!removidos.isEmpty()) {
            removerMinicursosDosDias(pessoaId, removidos);
        }
        return buscar(pessoaId);
    }

    /* O ingresso da pessoa vale neste dia? Quem não é diarista entra em
       todos. */
    @Transactional(readOnly = true)
    public boolean ingressoValeNoDia(Pessoa pessoa, LocalDate dia) {
        return !ehDiarista(pessoa) || pessoaDiaIngressoRepository.existsByPessoaIdAndDia(pessoa.getId(), dia);
    }

    @Transactional(readOnly = true)
    public List<LocalDate> diasEscolhidos(Integer pessoaId) {
        return pessoaDiaIngressoRepository.findByPessoaIdOrderByDiaAsc(pessoaId).stream()
                .map(PessoaDiaIngresso::getDia)
                .toList();
    }

    public boolean ehDiarista(Pessoa pessoa) {
        return pessoa.getTipoInscricao() != null
                && Boolean.TRUE.equals(pessoa.getTipoInscricao().getPorDia());
    }

    /* 2026-10-26 → 'SEG 26/10', o formato das mensagens de erro. */
    public static String formatarDia(LocalDate dia) {
        return ROTULOS_DIA_SEMANA[dia.getDayOfWeek().getValue() - 1] + " " + dia.format(DIA_MES);
    }

    /* ── Auxiliares ─────────────────────────────────────────────── */

    private Set<LocalDate> diasDisponiveis() {
        return eventoRepository.findAll().stream()
                .map(Evento::getDataHoraInicio)
                .filter(inicio -> inicio != null)
                .map(inicio -> inicio.toLocalDate())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /* dias_inscricao vazio num ingresso por dia é cadastro antigo ou
       manual; vale como uma diária, o mínimo que alguém pode ter pago. */
    private int diasContratados(Pessoa pessoa) {
        Integer dias = pessoa.getDiasInscricao();
        return dias == null || dias < 1 ? 1 : dias;
    }

    /* Só inscrições ainda não usadas (INSCRITO) em eventos que exigem
       inscrição — presença registrada é histórico e fica. */
    private void removerMinicursosDosDias(Integer pessoaId, Set<LocalDate> dias) {
        List<EventoParticipante> aRemover = eventoParticipanteRepository.buscarComEventoPorParticipante(pessoaId).stream()
                .filter(inscricao -> inscricao.getStatus() == StatusPresenca.INSCRITO)
                .filter(inscricao -> Boolean.TRUE.equals(inscricao.getEvento().getTipoEvento().getExigeInscricao()))
                .filter(inscricao -> dias.contains(inscricao.getEvento().getDataHoraInicio().toLocalDate()))
                .toList();
        eventoParticipanteRepository.deleteAll(aRemover);
    }

    private Pessoa buscarPessoa(Integer pessoaId) {
        return pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    }
}
