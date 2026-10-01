package com.semac.java_api.service;

import com.semac.java_api.dto.GanhadorSorteioDTO;
import com.semac.java_api.dto.ParticipanteElegivelDTO;
import com.semac.java_api.dto.SorteioRequestDTO;
import com.semac.java_api.dto.SorteioResponseDTO;
import com.semac.java_api.model.Brinde;
import com.semac.java_api.model.Evento;
import com.semac.java_api.model.EventoParticipante;
import com.semac.java_api.model.GanhadoresSorteio;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.Sorteio;
import com.semac.java_api.model.enums.StatusPresenca;
import com.semac.java_api.repository.BrindeRepository;
import com.semac.java_api.repository.EventoParticipanteRepository;
import com.semac.java_api.repository.EventoRepository;
import com.semac.java_api.repository.GanhadoresSorteioRepository;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.repository.SorteioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/* Regras dos sorteios de brindes (tabelas `sorteio`, `brinde` e
   `ganhadores_sorteio`).

   Sorteio é um cadastro (nome + evento); cada brinde pertence a um
   sorteio e cada unidade entregue vira uma linha em `ganhadores_sorteio`,
   com quem ganhou e quem realizou (organizador, vindo do token).

   A pool de elegíveis é calculada na hora, nunca persistida: pessoas com
   presença confirmada (`status = PRESENTE`) no evento do sorteio,
   excluindo quem já ganhou qualquer brinde na semana. O giro em si (nome
   sorteado, "ausente, girar de novo") é decidido no front; só a
   confirmação final grava aqui. */
@Service
public class SorteioService {

    private final EventoParticipanteRepository eventoParticipanteRepository;
    private final GanhadoresSorteioRepository ganhadoresSorteioRepository;
    private final SorteioRepository sorteioRepository;
    private final EventoRepository eventoRepository;
    private final BrindeRepository brindeRepository;
    private final PessoaRepository pessoaRepository;

    public SorteioService(EventoParticipanteRepository eventoParticipanteRepository,
                          GanhadoresSorteioRepository ganhadoresSorteioRepository,
                          SorteioRepository sorteioRepository,
                          EventoRepository eventoRepository,
                          BrindeRepository brindeRepository,
                          PessoaRepository pessoaRepository) {
        this.eventoParticipanteRepository = eventoParticipanteRepository;
        this.ganhadoresSorteioRepository = ganhadoresSorteioRepository;
        this.sorteioRepository = sorteioRepository;
        this.eventoRepository = eventoRepository;
        this.brindeRepository = brindeRepository;
        this.pessoaRepository = pessoaRepository;
    }

    // ── CRUD ────────────────────────────────────────────────────────

    /* Ordenado pelo início do evento, depois pelo nome — mesma ordem em
       que os sorteios acontecem na semana. */
    @Transactional(readOnly = true)
    public List<SorteioResponseDTO> listar() {
        return sorteioRepository.findAll().stream()
                .sorted(Comparator
                        .comparing((Sorteio s) -> s.getEvento().getDataHoraInicio())
                        .thenComparing(s -> s.getNome().toLowerCase()))
                .map(this::paraResposta)
                .toList();
    }

    @Transactional
    public SorteioResponseDTO criar(SorteioRequestDTO dto) {
        Sorteio sorteio = new Sorteio();
        sorteio.setNome(dto.nome().trim());
        sorteio.setEvento(buscarEvento(dto.eventoId()));
        return paraResposta(sorteioRepository.save(sorteio));
    }

    /* Trocar o evento depois de haver entregas mudaria, retroativamente,
       onde aqueles ganhadores estavam presentes — por isso é barrado. */
    @Transactional
    public SorteioResponseDTO atualizar(Integer id, SorteioRequestDTO dto) {
        Sorteio sorteio = buscarSorteio(id);
        boolean trocouEvento = !sorteio.getEvento().getId().equals(dto.eventoId());
        if (trocouEvento && ganhadoresSorteioRepository.countBySorteio_Id(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse sorteio já tem ganhadores — o evento não pode mais ser trocado.");
        }
        sorteio.setNome(dto.nome().trim());
        if (trocouEvento) {
            sorteio.setEvento(buscarEvento(dto.eventoId()));
        }
        return paraResposta(sorteioRepository.save(sorteio));
    }

    /* Com brindes vinculados é barrado: o brinde exige um sorteio
       (sorteio_id NOT NULL), então é preciso movê-los ou excluí-los antes. */
    @Transactional
    public void excluir(Integer id) {
        Sorteio sorteio = buscarSorteio(id);
        if (brindeRepository.existsBySorteio_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse sorteio tem brindes vinculados. Mova ou exclua os brindes antes.");
        }
        sorteioRepository.delete(sorteio);
    }

    // ── Realização ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ParticipanteElegivelDTO> elegiveis(Integer sorteioId) {
        Sorteio sorteio = buscarSorteio(sorteioId);
        return eventoParticipanteRepository
                .findByPk_EventoIdAndStatus(sorteio.getEvento().getId(), StatusPresenca.PRESENTE).stream()
                .map(EventoParticipante::getParticipante)
                .filter(participante -> !ganhadoresSorteioRepository.existsByParticipante_Id(participante.getId()))
                .map(p -> new ParticipanteElegivelDTO(p.getId(), p.getNome()))
                .toList();
    }

    /* Confirma o ganhador: valida vínculo do brinde, elegibilidade e
       estoque de novo aqui (nunca confiar só na lista que o front já
       buscou) e grava a entrega com o organizador. */
    @Transactional
    public GanhadorSorteioDTO registrarGanhador(Integer sorteioId, Integer brindeId, Integer participanteId,
                                                Integer organizadorId) {
        Sorteio sorteio = buscarSorteio(sorteioId);
        Brinde brinde = brindeRepository.findById(brindeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brinde não encontrado."));
        Pessoa participante = pessoaRepository.findById(participanteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participante não encontrado."));

        if (!brinde.getSorteio().getId().equals(sorteioId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse brinde não pertence a esse sorteio.");
        }
        boolean presente = eventoParticipanteRepository
                .findByPk_EventoIdAndStatus(sorteio.getEvento().getId(), StatusPresenca.PRESENTE).stream()
                .anyMatch(ep -> ep.getParticipante().getId().equals(participanteId));
        if (!presente) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse participante não está com presença confirmada no evento do sorteio.");
        }
        if (ganhadoresSorteioRepository.existsByParticipante_Id(participanteId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse participante já ganhou um brinde.");
        }
        if (ganhadoresSorteioRepository.countByBrinde_Id(brindeId) >= brinde.getQuantidade()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse brinde está esgotado.");
        }

        GanhadoresSorteio entrega = new GanhadoresSorteio();
        entrega.setSorteio(sorteio);
        entrega.setBrinde(brinde);
        entrega.setParticipante(participante);
        entrega.setOrganizador(pessoaRepository.getReferenceById(organizadorId));
        entrega.setGanhouEm(LocalDateTime.now());
        entrega = ganhadoresSorteioRepository.save(entrega);

        return paraGanhador(entrega);
    }

    // ── Auxiliares ──────────────────────────────────────────────────

    private Sorteio buscarSorteio(Integer id) {
        return sorteioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sorteio não encontrado."));
    }

    private Evento buscarEvento(Integer id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));
    }

    private SorteioResponseDTO paraResposta(Sorteio sorteio) {
        List<GanhadorSorteioDTO> ganhadores = sorteio.getId() == null
                ? List.of()
                : ganhadoresSorteioRepository.findBySorteio_IdOrderByGanhouEmAsc(sorteio.getId()).stream()
                        .map(this::paraGanhador)
                        .toList();
        int quantidadeBrindes = brindeRepository.findBySorteio_Id(sorteio.getId()).stream()
                .mapToInt(Brinde::getQuantidade)
                .sum();
        Evento evento = sorteio.getEvento();
        return new SorteioResponseDTO(
                sorteio.getId(),
                sorteio.getNome(),
                evento.getId(),
                evento.getNome(),
                evento.getDataHoraInicio(),
                quantidadeBrindes,
                ganhadores.size(),
                ganhadores);
    }

    private GanhadorSorteioDTO paraGanhador(GanhadoresSorteio entrega) {
        return new GanhadorSorteioDTO(
                entrega.getId(),
                entrega.getBrinde().getNome(),
                entrega.getParticipante().getNome(),
                entrega.getOrganizador().getNome(),
                entrega.getGanhouEm());
    }
}
