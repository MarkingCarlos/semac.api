package com.semac.java_api.service;

import com.semac.java_api.dto.CamisetaParticipanteDTO;
import com.semac.java_api.dto.InscricaoFinanceiraDTO;
import com.semac.java_api.dto.ParticipanteResponseDTO;
import com.semac.java_api.dto.PresencaParticipanteDTO;
import com.semac.java_api.dto.TipoInscricaoResponseDTO;
import com.semac.java_api.model.CamisaPedido;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.TipoInscricao;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.repository.TipoInscricaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PessoaService {

    private final PessoaRepository pessoaRepository;
    private final TipoInscricaoRepository tipoInscricaoRepository;

    public PessoaService(PessoaRepository pessoaRepository,
                         TipoInscricaoRepository tipoInscricaoRepository) {
        this.pessoaRepository = pessoaRepository;
        this.tipoInscricaoRepository = tipoInscricaoRepository;
    }

    /* Participantes do /admin: confirmados (role = PARTICIPANTE) e os
       recém-inscritos aguardando confirmação (role = NULL). @Transactional
       para permitir o acesso lazy a eventoParticipantes/camisaPedidos. */
    @Transactional(readOnly = true)
    public List<ParticipanteResponseDTO> listarParticipantes() {
        return pessoaRepository.findByRoleIsNullOrRoleOrderByNomeAsc(Role.PARTICIPANTE).stream()
                .map(this::paraResposta)
                .toList();
    }

    /* Comissão organizadora do /admin: todas as pessoas com papel definido
       diferente de PARTICIPANTE (MEMBRO, DIRETOR_* e PRESIDENTE). role=NULL
       fica de fora (ainda aguardando confirmação). */
    @Transactional(readOnly = true)
    public List<ParticipanteResponseDTO> listarComissao() {
        return pessoaRepository.findAllByRoleNot(Role.PARTICIPANTE).stream()
                .sorted(java.util.Comparator.comparing(Pessoa::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(this::paraResposta)
                .toList();
    }

    /* Inscrições confirmadas para o módulo financeiro: participantes
       (role = PARTICIPANTE) com ingresso definido. Valor vem do ingresso. */
    @Transactional(readOnly = true)
    public List<InscricaoFinanceiraDTO> listarInscricoes() {
        return pessoaRepository.findAllByRole(Role.PARTICIPANTE).stream()
                .filter(pessoa -> pessoa.getTipoInscricao() != null)
                .map(pessoa -> new InscricaoFinanceiraDTO(
                        pessoa.getId(),
                        pessoa.getNome(),
                        pessoa.getTipoInscricao().getNome(),
                        pessoa.getTipoInscricao().getValor(),
                        pessoa.getTipoInscricao().getAno()))
                .toList();
    }

    /* Confirmação da inscrição: atribui o papel da pessoa. Aceita
       PARTICIPANTE ou qualquer papel de comissão (MEMBRO, DIRETOR_* e
       PRESIDENTE). Para PARTICIPANTE, exige um tipo de ingresso válido;
       para papéis de comissão, o ingresso é limpo. */
    @Transactional
    public ParticipanteResponseDTO atribuirRole(Integer id, Role role, Integer tipoInscricaoId) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Participante não encontrado."));

        if (role == Role.PARTICIPANTE) {
            if (tipoInscricaoId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Selecione o tipo de ingresso do participante.");
            }
            TipoInscricao tipo = tipoInscricaoRepository.findById(tipoInscricaoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Tipo de ingresso inválido."));
            pessoa.setTipoInscricao(tipo);
        } else {
            pessoa.setTipoInscricao(null);
        }

        pessoa.setRole(role);
        return paraResposta(pessoaRepository.save(pessoa));
    }

    /* Ativa/desativa uma pessoa (ex.: suspender membro da comissão). */
    @Transactional
    public ParticipanteResponseDTO definirAtivo(Integer id, boolean ativo) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Pessoa não encontrada."));
        pessoa.setAtivo(ativo);
        return paraResposta(pessoaRepository.save(pessoa));
    }

    private ParticipanteResponseDTO paraResposta(Pessoa pessoa) {
        List<PresencaParticipanteDTO> presencas = pessoa.getEventoParticipantes().stream()
                .map(ep -> new PresencaParticipanteDTO(ep.getStatus().name()))
                .toList();

        // Camiseta da inscrição: usa o primeiro pedido registrado (se houver).
        CamisetaParticipanteDTO camiseta = pessoa.getCamisaPedidos().stream()
                .findFirst()
                .map(this::paraCamiseta)
                .orElse(null);

        TipoInscricao ingresso = pessoa.getTipoInscricao();
        TipoInscricaoResponseDTO tipoInscricao = ingresso == null ? null
                : new TipoInscricaoResponseDTO(
                        ingresso.getId(), ingresso.getNome(), ingresso.getValor(),
                        ingresso.getAno(), ingresso.getAtivo());

        return new ParticipanteResponseDTO(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getEmail(),
                pessoa.getRa(),
                pessoa.getAtivo(),
                pessoa.getRole() == null ? null : pessoa.getRole().name(),
                camiseta,
                tipoInscricao,
                presencas
        );
    }

    private CamisetaParticipanteDTO paraCamiseta(CamisaPedido pedido) {
        return new CamisetaParticipanteDTO(pedido.getModelo().name(), pedido.getTamanho().name());
    }
}
