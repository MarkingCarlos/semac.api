package com.semac.java_api.service;

import com.semac.java_api.dto.AtualizarPerfilDTO;
import com.semac.java_api.dto.CamisetaParticipanteDTO;
import com.semac.java_api.dto.InscricaoFinanceiraDTO;
import com.semac.java_api.dto.NivelResponseDTO;
import com.semac.java_api.dto.ParticipanteResponseDTO;
import com.semac.java_api.dto.PerfilResponseDTO;
import com.semac.java_api.dto.PresencaParticipanteDTO;
import com.semac.java_api.dto.TipoInscricaoResponseDTO;
import com.semac.java_api.model.CamisaPedido;
import com.semac.java_api.model.Nivel;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.TipoInscricao;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.repository.CamisaPedidoRepository;
import com.semac.java_api.repository.NivelRepository;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.repository.TipoInscricaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PessoaService {

    /* Xp de boas-vindas atribuído na confirmação da inscrição. Valor
       provisório — ajustar quando a diretoria decidir o XP inicial real.
       Funciona com qualquer nível cadastrado com xpMinimo 0, já que
       nesse caso qualquer xp não-negativo já cai no primeiro nível. */
    private static final int XP_INICIAL_CONFIRMACAO = 100;

    private final PessoaRepository pessoaRepository;
    private final TipoInscricaoRepository tipoInscricaoRepository;
    private final CamisaPedidoRepository camisaPedidoRepository;
    private final NivelRepository nivelRepository;
    private final InscricaoEventoService inscricaoEventoService;

    public PessoaService(PessoaRepository pessoaRepository,
                         TipoInscricaoRepository tipoInscricaoRepository,
                         CamisaPedidoRepository camisaPedidoRepository,
                         NivelRepository nivelRepository,
                         InscricaoEventoService inscricaoEventoService) {
        this.pessoaRepository = pessoaRepository;
        this.tipoInscricaoRepository = tipoInscricaoRepository;
        this.camisaPedidoRepository = camisaPedidoRepository;
        this.nivelRepository = nivelRepository;
        this.inscricaoEventoService = inscricaoEventoService;
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
       PRESIDENTE). Para PARTICIPANTE, exige um tipo de ingresso válido
       e atribui o xp de boas-vindas + o nível correspondente; para
       papéis de comissão, ingresso/xp/nível são limpos. */
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

            Nivel nivelInicial = nivelRepository
                    .findTopByXpMinimoLessThanEqualOrderByXpMinimoDesc(XP_INICIAL_CONFIRMACAO)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Cadastre ao menos um nível em Informações SEMAC antes de confirmar participantes."));
            pessoa.setXp(XP_INICIAL_CONFIRMACAO);
            pessoa.setNivel(nivelInicial);
        } else {
            pessoa.setTipoInscricao(null);
            pessoa.setXp(null);
            pessoa.setNivel(null);
        }

        pessoa.setRole(role);
        Pessoa salva = pessoaRepository.save(pessoa);

        /* Participante confirmado já entra em todos os eventos abertos
           (palestra, mesa redonda, debate); quem vira comissão sai deles,
           porque organizador não pontua nem ocupa vaga. Minicurso fica
           de fora: é escolha do participante na área /participantes. */
        if (role == Role.PARTICIPANTE) {
            inscricaoEventoService.preInscreverEmEventosAbertos(salva);
        } else {
            inscricaoEventoService.removerInscricoesDoParticipante(salva.getId());
        }

        return paraResposta(salva);
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

    /* Perfil do próprio usuário logado (seção Início do /admin). O id vem
       da claim do token, então a pessoa sempre existe — 404 só cobre o
       caso de um token com id órfão. */
    @Transactional(readOnly = true)
    public PerfilResponseDTO buscarPerfil(Integer id) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuário não encontrado."));
        return paraPerfil(pessoa);
    }

    /* Atualiza os campos editáveis do próprio perfil: RA e a camiseta.
       A comissão sempre tem um pedido de camiseta (escolhido na inscrição);
       o orElse cria um só como salvaguarda. */
    @Transactional
    public PerfilResponseDTO atualizarPerfil(Integer id, AtualizarPerfilDTO dto) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuário não encontrado."));

        String ra = dto.ra() == null || dto.ra().isBlank() ? null : dto.ra().trim();
        pessoa.setRa(ra);

        CamisaPedido pedido = pessoa.getCamisaPedidos().stream()
                .findFirst()
                .orElseGet(() -> {
                    CamisaPedido novo = new CamisaPedido();
                    novo.setPessoa(pessoa);
                    return novo;
                });
        pedido.setModelo(dto.modelo());
        pedido.setTamanho(dto.tamanho());
        camisaPedidoRepository.save(pedido);

        pessoaRepository.save(pessoa);

        // Constrói a resposta a partir do pedido salvo (a coleção lazy da
        // pessoa pode não refletir um pedido recém-criado).
        return new PerfilResponseDTO(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getEmail(),
                pessoa.getRole() == null ? null : pessoa.getRole().name(),
                pessoa.getRa(),
                paraCamiseta(pedido)
        );
    }

    private PerfilResponseDTO paraPerfil(Pessoa pessoa) {
        CamisetaParticipanteDTO camiseta = pessoa.getCamisaPedidos().stream()
                .findFirst()
                .map(this::paraCamiseta)
                .orElse(null);

        return new PerfilResponseDTO(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getEmail(),
                pessoa.getRole() == null ? null : pessoa.getRole().name(),
                pessoa.getRa(),
                camiseta
        );
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

        Nivel nivel = pessoa.getNivel();
        NivelResponseDTO nivelResponse = nivel == null ? null
                : new NivelResponseDTO(nivel.getId(), nivel.getNome(), nivel.getXpMinimo());

        return new ParticipanteResponseDTO(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getEmail(),
                pessoa.getRa(),
                pessoa.getAtivo(),
                pessoa.getRole() == null ? null : pessoa.getRole().name(),
                camiseta,
                tipoInscricao,
                nivelResponse,
                pessoa.getXp(),
                presencas
        );
    }

    private CamisetaParticipanteDTO paraCamiseta(CamisaPedido pedido) {
        return new CamisetaParticipanteDTO(pedido.getModelo().name(), pedido.getTamanho().name());
    }
}
