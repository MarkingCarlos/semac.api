package com.semac.java_api.service;

import com.semac.java_api.dto.ConquistaDoParticipanteDTO;
import com.semac.java_api.dto.NivelResponseDTO;
import com.semac.java_api.dto.PresencaResumoParticipanteDTO;
import com.semac.java_api.dto.ResumoParticipanteAdminDTO;
import com.semac.java_api.dto.TimeResumoParticipanteDTO;
import com.semac.java_api.model.EventoParticipante;
import com.semac.java_api.model.Nivel;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.enums.StatusPresenca;
import com.semac.java_api.repository.EventoParticipanteRepository;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.repository.TimeIntegranteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/* Monta a página de resumo de um participante no /admin (botão "Resumo"
   da aba Pessoas -> Participantes). Só leitura: revogar conquista segue
   no modal da tabela.

   Fica fora do PessoaService para não somar mais dependências a ele —
   aqui só se junta o que outras partes já guardam (xp/nível da pessoa,
   time do sorteio, conquistas e check-ins). */
@Service
public class ResumoParticipanteService {

    private final PessoaRepository pessoaRepository;
    private final TimeIntegranteRepository timeIntegranteRepository;
    private final EventoParticipanteRepository eventoParticipanteRepository;
    private final ConquistaService conquistaService;

    public ResumoParticipanteService(PessoaRepository pessoaRepository,
                                     TimeIntegranteRepository timeIntegranteRepository,
                                     EventoParticipanteRepository eventoParticipanteRepository,
                                     ConquistaService conquistaService) {
        this.pessoaRepository = pessoaRepository;
        this.timeIntegranteRepository = timeIntegranteRepository;
        this.eventoParticipanteRepository = eventoParticipanteRepository;
        this.conquistaService = conquistaService;
    }

    @Transactional(readOnly = true)
    public ResumoParticipanteAdminDTO resumoParaAdmin(Integer participanteId) {
        Pessoa pessoa = pessoaRepository.findById(participanteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Participante não encontrado"));

        Nivel nivel = pessoa.getNivel();
        NivelResponseDTO nivelResponse = nivel == null ? null
                : new NivelResponseDTO(nivel.getId(), nivel.getNome(), nivel.getXpMinimo());

        TimeResumoParticipanteDTO time = timeIntegranteRepository.findById(participanteId)
                .map(integrante -> new TimeResumoParticipanteDTO(
                        integrante.getTime().getId(),
                        integrante.getTime().getNome(),
                        integrante.getTime().getCor()))
                .orElse(null);

        List<ConquistaDoParticipanteDTO> conquistas = conquistaService.listarDoParticipanteParaAdmin(participanteId)
                .stream()
                .sorted(Comparator.comparing(ConquistaDoParticipanteDTO::obtidaEm,
                        Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .toList();

        List<PresencaResumoParticipanteDTO> presencas = eventoParticipanteRepository
                .buscarComEventoPorParticipante(participanteId).stream()
                .filter(vinculo -> vinculo.getStatus() == StatusPresenca.PRESENTE)
                .sorted(Comparator.comparing(EventoParticipante::getPresencaEm,
                        Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .map(vinculo -> new PresencaResumoParticipanteDTO(
                        vinculo.getEvento().getId(),
                        vinculo.getEvento().getNome(),
                        vinculo.getEvento().getTipoEvento().getNome(),
                        vinculo.getEvento().getDataHoraInicio(),
                        vinculo.getPresencaEm(),
                        vinculo.getXpCreditado(),
                        vinculo.getRegistradoPorNome()))
                .toList();

        return new ResumoParticipanteAdminDTO(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getEmail(),
                pessoa.getXp(),
                nivelResponse,
                time,
                conquistas,
                presencas);
    }
}
