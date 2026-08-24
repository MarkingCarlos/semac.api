package com.semac.java_api.service;

import com.semac.java_api.dto.ParticipanteElegivelDTO;
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
import java.util.List;

/* Regras do sorteio de brindes (tabelas `sorteio` / `ganhadores_sorteio`).

   A pool de elegíveis é calculada na hora, nunca persistida: pessoas com
   presença confirmada (`status = PRESENTE`) no evento escolhido,
   excluindo quem já consta em `ganhadores_sorteio` — quem já ganhou
   qualquer brinde fica de fora de todos os sorteios seguintes da semana
   (ver documentação do banco). O giro em si (nome sorteado, "ausente,
   girar de novo") é decidido no front; só a confirmação final grava aqui. */
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

    @Transactional(readOnly = true)
    public List<ParticipanteElegivelDTO> elegiveis(Integer eventoId) {
        return eventoParticipanteRepository.findByPk_EventoIdAndStatus(eventoId, StatusPresenca.PRESENTE).stream()
                .map(EventoParticipante::getParticipante)
                .filter(participante -> !ganhadoresSorteioRepository.existsByParticipante_Id(participante.getId()))
                .map(p -> new ParticipanteElegivelDTO(p.getId(), p.getNome()))
                .toList();
    }

    /* Confirma o ganhador: valida elegibilidade e estoque de novo aqui
       (nunca confiar só na lista que o front já buscou) e grava o
       sorteio + o ganhador numa transação só. */
    @Transactional
    public SorteioResponseDTO registrarGanhador(Integer eventoId, Integer brindeId, Integer participanteId,
                                                Integer organizadorId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));
        Brinde brinde = brindeRepository.findById(brindeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brinde não encontrado."));
        Pessoa participante = pessoaRepository.findById(participanteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participante não encontrado."));

        boolean presente = eventoParticipanteRepository
                .findByPk_EventoIdAndStatus(eventoId, StatusPresenca.PRESENTE).stream()
                .anyMatch(ep -> ep.getParticipante().getId().equals(participanteId));
        if (!presente) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse participante não está com presença confirmada nesse evento.");
        }
        if (ganhadoresSorteioRepository.existsByParticipante_Id(participanteId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse participante já ganhou um brinde.");
        }
        if (sorteioRepository.countByBrinde_Id(brindeId) >= brinde.getQuantidade()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse brinde está esgotado.");
        }

        Sorteio sorteio = new Sorteio();
        sorteio.setEvento(evento);
        sorteio.setBrinde(brinde);
        sorteio.setOrganizador(pessoaRepository.getReferenceById(organizadorId));
        sorteio.setRealizadoEm(LocalDateTime.now());
        sorteio = sorteioRepository.save(sorteio);

        GanhadoresSorteio ganhador = new GanhadoresSorteio();
        ganhador.setSorteio(sorteio);
        ganhador.setParticipante(participante);
        ganhador.setGanhouEm(LocalDateTime.now());
        ganhadoresSorteioRepository.save(ganhador);

        return new SorteioResponseDTO(sorteio.getId(), participante.getNome(), sorteio.getRealizadoEm());
    }
}
