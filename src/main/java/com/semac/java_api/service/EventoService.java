package com.semac.java_api.service;

import com.semac.java_api.dto.EventoRequestDTO;
import com.semac.java_api.dto.EventoResponseDTO;
import com.semac.java_api.dto.PalestranteDTO;
import com.semac.java_api.dto.TipoEventoResponseDTO;
import com.semac.java_api.model.Evento;
import com.semac.java_api.model.EventoPalestrante;
import com.semac.java_api.model.Palestrante;
import com.semac.java_api.model.TipoEvento;
import com.semac.java_api.model.pk.EventoPalestrantePK;
import com.semac.java_api.repository.EventoPalestranteRepository;
import com.semac.java_api.repository.EventoRepository;
import com.semac.java_api.repository.PalestranteRepository;
import com.semac.java_api.repository.TipoEventoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final TipoEventoRepository tipoEventoRepository;
    private final PalestranteRepository palestranteRepository;
    private final EventoPalestranteRepository eventoPalestranteRepository;

    public EventoService(EventoRepository eventoRepository,
                         TipoEventoRepository tipoEventoRepository,
                         PalestranteRepository palestranteRepository,
                         EventoPalestranteRepository eventoPalestranteRepository) {
        this.eventoRepository = eventoRepository;
        this.tipoEventoRepository = tipoEventoRepository;
        this.palestranteRepository = palestranteRepository;
        this.eventoPalestranteRepository = eventoPalestranteRepository;
    }

    @Transactional(readOnly = true)
    public List<EventoResponseDTO> listar() {
        return eventoRepository.findAll().stream()
                .sorted(Comparator.comparing(Evento::getDataHoraInicio))
                .map(this::paraResposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventoResponseDTO buscar(Integer id) {
        return paraResposta(carregar(id));
    }

    @Transactional
    public EventoResponseDTO criar(EventoRequestDTO dto) {
        Evento evento = new Evento();
        aplicar(evento, dto);
        eventoRepository.save(evento);
        sincronizarPalestrantes(evento, dto.palestrantes());
        return paraResposta(evento);
    }

    @Transactional
    public EventoResponseDTO atualizar(Integer id, EventoRequestDTO dto) {
        Evento evento = carregar(id);
        aplicar(evento, dto);
        eventoRepository.save(evento);
        sincronizarPalestrantes(evento, dto.palestrantes());
        return paraResposta(evento);
    }

    @Transactional
    public void excluir(Integer id) {
        Evento evento = carregar(id);
        removerPalestrantes(evento);
        eventoRepository.delete(evento);
    }

    /* ── Auxiliares ─────────────────────────────────────────────── */

    private Evento carregar(Integer id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Evento não encontrado."));
    }

    private void aplicar(Evento evento, EventoRequestDTO dto) {
        TipoEvento tipoEvento = tipoEventoRepository.findById(dto.tipoEventoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tipo de evento inválido."));

        evento.setNome(dto.nome());
        evento.setTipoEvento(tipoEvento);
        evento.setLocal(dto.local());
        evento.setDescricao(dto.descricao());
        evento.setDataHoraInicio(dto.dataHoraInicio());
        evento.setDataHoraFim(dto.dataHoraFim());
        evento.setCapacidadeMaxima(dto.capacidadeMaxima());
    }

    /* Palestrantes são "inline" do evento (sem reuso na interface): a cada
       gravação removemos os vínculos/palestrantes antigos e recriamos a
       partir do formulário. */
    private void sincronizarPalestrantes(Evento evento, List<PalestranteDTO> palestrantes) {
        removerPalestrantes(evento);
        if (palestrantes == null) {
            return;
        }
        for (PalestranteDTO dto : palestrantes) {
            if (dto.nome() == null || dto.nome().isBlank()) {
                continue;
            }
            Palestrante palestrante = new Palestrante();
            palestrante.setNome(dto.nome());
            palestrante.setDescricao(dto.descricao());
            palestranteRepository.save(palestrante);

            EventoPalestrante vinculo = new EventoPalestrante();
            vinculo.setPk(new EventoPalestrantePK(evento.getId(), palestrante.getId()));
            vinculo.setEvento(evento);
            vinculo.setPalestrante(palestrante);
            eventoPalestranteRepository.save(vinculo);
        }
    }

    /* Remove os vínculos do evento e os palestrantes que ficarem órfãos
       (sem nenhum outro evento ligado). */
    private void removerPalestrantes(Evento evento) {
        List<EventoPalestrante> vinculos = eventoPalestranteRepository.findByPk_EventoId(evento.getId());
        eventoPalestranteRepository.deleteAll(vinculos);
        eventoPalestranteRepository.flush();
        for (EventoPalestrante vinculo : vinculos) {
            Integer palestranteId = vinculo.getPk().getPalestranteId();
            if (eventoPalestranteRepository.findByPk_PalestranteId(palestranteId).isEmpty()) {
                palestranteRepository.deleteById(palestranteId);
            }
        }
    }

    private EventoResponseDTO paraResposta(Evento evento) {
        TipoEvento tipo = evento.getTipoEvento();
        TipoEventoResponseDTO tipoEvento = tipo == null ? null
                : new TipoEventoResponseDTO(tipo.getId(), tipo.getNome(), tipo.getPontos());

        // A partir do repositório (não da coleção em memória) para refletir
        // os vínculos recém-criados na mesma transação.
        List<PalestranteDTO> palestrantes = eventoPalestranteRepository.findByPk_EventoId(evento.getId()).stream()
                .map(EventoPalestrante::getPalestrante)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(Palestrante::getId))
                .map(p -> new PalestranteDTO(p.getId(), p.getNome(), p.getDescricao()))
                .toList();

        return new EventoResponseDTO(
                evento.getId(),
                evento.getNome(),
                tipoEvento,
                evento.getLocal(),
                evento.getDescricao(),
                evento.getDataHoraInicio(),
                evento.getDataHoraFim(),
                evento.getCapacidadeMaxima(),
                palestrantes
        );
    }
}
