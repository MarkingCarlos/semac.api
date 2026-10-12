package com.semac.java_api.service;

import com.semac.java_api.dto.PerguntaDebateResponseDTO;
import com.semac.java_api.model.ConfiguracaoInscricao;
import com.semac.java_api.model.Evento;
import com.semac.java_api.model.PerguntaDebate;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.enums.CodigoTipoEvento;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.repository.ConfiguracaoInscricaoRepository;
import com.semac.java_api.repository.EventoRepository;
import com.semac.java_api.repository.PerguntaDebateRepository;
import com.semac.java_api.repository.PessoaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/* Perguntas enviadas previamente para os debates. O participante envia
   pelo /participantes (quantas quiser, até o limite abaixo); a diretoria
   e a presidência leem no /admin, aba Conteúdo. */
@Service
public class PerguntaDebateService {

    /* Teto de perguntas por participante em um mesmo debate — folgado para
       uso real, mas impede que alguém encha a lista do /admin. */
    static final int LIMITE_PERGUNTAS_POR_PARTICIPANTE = 20;

    private final PerguntaDebateRepository perguntaDebateRepository;
    private final EventoRepository eventoRepository;
    private final PessoaRepository pessoaRepository;
    private final ConfiguracaoInscricaoRepository configuracaoInscricaoRepository;

    public PerguntaDebateService(PerguntaDebateRepository perguntaDebateRepository,
                                 EventoRepository eventoRepository,
                                 PessoaRepository pessoaRepository,
                                 ConfiguracaoInscricaoRepository configuracaoInscricaoRepository) {
        this.perguntaDebateRepository = perguntaDebateRepository;
        this.eventoRepository = eventoRepository;
        this.pessoaRepository = pessoaRepository;
        this.configuracaoInscricaoRepository = configuracaoInscricaoRepository;
    }

    @Transactional
    public void enviar(Integer participanteId, Integer eventoId, String texto) {
        Pessoa pessoa = pessoaRepository.findById(participanteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
        if (pessoa.getRole() != Role.PARTICIPANTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Só participantes podem enviar perguntas.");
        }

        Evento evento = buscarDebate(eventoId);
        exigirPerguntasAbertas(evento);

        if (perguntaDebateRepository.countByEventoIdAndPessoaId(eventoId, participanteId)
                >= LIMITE_PERGUNTAS_POR_PARTICIPANTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você já enviou o máximo de " + LIMITE_PERGUNTAS_POR_PARTICIPANTE
                            + " perguntas para este debate.");
        }

        PerguntaDebate pergunta = new PerguntaDebate();
        pergunta.setEvento(evento);
        pergunta.setPessoa(pessoa);
        pergunta.setTexto(texto.trim());
        pergunta.setEnviadaEm(LocalDateTime.now());
        perguntaDebateRepository.save(pergunta);
    }

    /* Mais antigas primeiro: é a ordem em que a mediação costuma ler. */
    @Transactional(readOnly = true)
    public List<PerguntaDebateResponseDTO> listar(Integer eventoId) {
        buscarDebate(eventoId);
        return perguntaDebateRepository.findByEventoIdOrderByEnviadaEmAsc(eventoId).stream()
                .map(pergunta -> new PerguntaDebateResponseDTO(
                        pergunta.getId(),
                        pergunta.getTexto(),
                        pergunta.getPessoa().getNome(),
                        pergunta.getEnviadaEm()))
                .toList();
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
    private void exigirPerguntasAbertas(Evento evento) {
        boolean abertas = configuracaoInscricaoRepository
                .findByAno(evento.getDataHoraInicio().getYear())
                .map(ConfiguracaoInscricao::getPerguntasDebateAbertas)
                .orElse(Boolean.FALSE);
        if (!abertas) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O envio de perguntas para os debates está encerrado.");
        }
    }
}
