package com.semac.java_api.service;

import com.semac.java_api.dto.AtualizarCamisetaPerfilDTO;
import com.semac.java_api.dto.AtualizarPerfilDTO;
import com.semac.java_api.dto.CamisetaAdminDTO;
import com.semac.java_api.dto.CamisetaParticipanteDTO;
import com.semac.java_api.dto.CamisetaPerfilDTO;
import com.semac.java_api.dto.InscricaoFinanceiraDTO;
import com.semac.java_api.dto.NivelResponseDTO;
import com.semac.java_api.dto.PagamentoCartaoInfoDTO;
import com.semac.java_api.dto.ParticipanteResponseDTO;
import com.semac.java_api.dto.PerfilResponseDTO;
import com.semac.java_api.dto.PresencaParticipanteDTO;
import com.semac.java_api.dto.RankingParticipanteDTO;
import com.semac.java_api.dto.RankingResponseDTO;
import com.semac.java_api.dto.TipoInscricaoResponseDTO;
import com.semac.java_api.model.CamisaPedido;
import com.semac.java_api.model.CamisetaExtra;
import com.semac.java_api.model.Nivel;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.TipoInscricao;
import com.semac.java_api.model.enums.FormaPagamento;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.repository.CaixaFundunespRepository;
import com.semac.java_api.repository.CamisaPedidoRepository;
import com.semac.java_api.repository.CamisetaExtraRepository;
import com.semac.java_api.repository.GanhadoresSorteioRepository;
import com.semac.java_api.repository.NivelRepository;
import com.semac.java_api.repository.ParticipanteConquistaRepository;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.repository.SorteioRepository;
import com.semac.java_api.repository.TipoInscricaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private final CamisetaExtraRepository camisetaExtraRepository;
    private final NivelRepository nivelRepository;
    private final InscricaoEventoService inscricaoEventoService;
    private final SorteioRepository sorteioRepository;
    private final CaixaFundunespRepository caixaFundunespRepository;
    private final ParticipanteConquistaRepository participanteConquistaRepository;
    private final GanhadoresSorteioRepository ganhadoresSorteioRepository;

    public PessoaService(PessoaRepository pessoaRepository,
                         TipoInscricaoRepository tipoInscricaoRepository,
                         CamisaPedidoRepository camisaPedidoRepository,
                         CamisetaExtraRepository camisetaExtraRepository,
                         NivelRepository nivelRepository,
                         InscricaoEventoService inscricaoEventoService,
                         SorteioRepository sorteioRepository,
                         CaixaFundunespRepository caixaFundunespRepository,
                         ParticipanteConquistaRepository participanteConquistaRepository,
                         GanhadoresSorteioRepository ganhadoresSorteioRepository) {
        this.pessoaRepository = pessoaRepository;
        this.tipoInscricaoRepository = tipoInscricaoRepository;
        this.camisaPedidoRepository = camisaPedidoRepository;
        this.camisetaExtraRepository = camisetaExtraRepository;
        this.nivelRepository = nivelRepository;
        this.inscricaoEventoService = inscricaoEventoService;
        this.sorteioRepository = sorteioRepository;
        this.caixaFundunespRepository = caixaFundunespRepository;
        this.participanteConquistaRepository = participanteConquistaRepository;
        this.ganhadoresSorteioRepository = ganhadoresSorteioRepository;
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
                        valorDaInscricao(pessoa),
                        pessoa.getTipoInscricao().getAno()))
                .toList();
    }

    /* Ingresso de diária é cobrado por dia: o valor cadastrado vale por
       diária e o total sai da quantidade escolhida no cadastro. */
    private BigDecimal valorDaInscricao(Pessoa pessoa) {
        TipoInscricao ingresso = pessoa.getTipoInscricao();
        if (!Boolean.TRUE.equals(ingresso.getPorDia())) {
            return ingresso.getValor();
        }
        int dias = pessoa.getDiasInscricao() == null ? 1 : pessoa.getDiasInscricao();
        return ingresso.getValor().multiply(BigDecimal.valueOf(dias));
    }

    /* Total real da inscrição: ingresso (valorDaInscricao acima) + camisetas
       avulsas × preço vigente no ano do ingresso. Usado só para cobrar o
       cartão (PagamentoCartaoService) — nunca confia em valor vindo do
       cliente. Se houver avulsa sem preço cadastrado para o ano, falha em
       vez de cobrar zero: subcobrar silenciosamente é pior que recusar. */
    @Transactional(readOnly = true)
    public BigDecimal calcularValorTotalInscricao(Pessoa pessoa) {
        BigDecimal totalIngresso = valorDaInscricao(pessoa);

        long avulsas = camisaPedidoRepository.countByPessoaIdAndAvulsaTrue(pessoa.getId());
        if (avulsas == 0) {
            return totalIngresso;
        }

        Integer ano = pessoa.getTipoInscricao().getAno();
        BigDecimal precoAvulsa = camisetaExtraRepository.findByAno(ano)
                .map(CamisetaExtra::getValor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Preço da camiseta avulsa não cadastrado para " + ano + "."));

        return totalIngresso.add(precoAvulsa.multiply(BigDecimal.valueOf(avulsas)));
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
            pessoa.setDiasInscricao(null);
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

    /* Exclui definitivamente um participante ou membro da comissão (ação
       irreversível — para preservar histórico, prefira "Desativar").
       Bloqueada se a pessoa for responsável por registros de auditoria que
       não podem ficar órfãos: sorteios que organizou (organizador_id é
       NOT NULL) e o último ajuste do caixa do Fundunesp. Os demais vínculos
       (camisetas, inscrições em eventos, conquistas e prêmios ganhos) são
       apagados junto, por serem exclusivos dessa pessoa. */
    @Transactional
    public void excluir(Integer id) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Pessoa não encontrada."));

        if (sorteioRepository.existsByOrganizador_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta pessoa organizou sorteios e não pode ser excluída. Desative-a em vez disso.");
        }
        if (caixaFundunespRepository.existsByAtualizadoPor_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta pessoa fez ajustes no caixa do Fundunesp e não pode ser excluída. Desative-a em vez disso.");
        }

        camisaPedidoRepository.deleteByPessoaId(id);
        inscricaoEventoService.removerInscricoesDoParticipante(id);
        participanteConquistaRepository.deleteByPk_ParticipanteId(id);
        ganhadoresSorteioRepository.deleteByParticipante_Id(id);
        pessoaRepository.delete(pessoa);
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

    /* Atualiza os campos editáveis do próprio perfil: RA e o modelo/tamanho
       das camisetas já existentes. Não cria nem apaga camiseta — o `id`
       enviado precisa bater exatamente com o conjunto de camisa_pedido da
       pessoa (nem a mais, nem a menos), senão 400. */
    @Transactional
    public PerfilResponseDTO atualizarPerfil(Integer id, AtualizarPerfilDTO dto) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuário não encontrado."));

        String ra = dto.ra() == null || dto.ra().isBlank() ? null : dto.ra().trim();
        pessoa.setRa(ra);
        pessoaRepository.save(pessoa);

        List<CamisaPedido> existentes = camisaPedidoRepository.findByPessoaId(id);
        if (existentes.size() != dto.camisetas().size()
                || !existentes.stream().map(CamisaPedido::getId)
                        .allMatch(existenteId -> dto.camisetas().stream()
                                .anyMatch(item -> item.id().equals(existenteId)))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é possível adicionar ou remover camisetas por aqui.");
        }

        for (AtualizarCamisetaPerfilDTO item : dto.camisetas()) {
            CamisaPedido pedido = existentes.stream()
                    .filter(p -> p.getId().equals(item.id()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Camiseta não encontrada."));
            pedido.setModelo(item.modelo());
            pedido.setTamanho(item.tamanho());
        }
        camisaPedidoRepository.saveAll(existentes);

        // Constrói a resposta a partir da lista recém-salva — a coleção
        // lazy da pessoa pode não refletir a atualização ainda nesta
        // transação (mesmo cuidado de atualizarCamisetas).
        List<CamisetaPerfilDTO> camisetas = existentes.stream().map(this::paraCamisetaPerfil).toList();
        return paraPerfil(pessoa, camisetas);
    }

    private PerfilResponseDTO paraPerfil(Pessoa pessoa) {
        List<CamisetaPerfilDTO> camisetas = pessoa.getCamisaPedidos().stream()
                .map(this::paraCamisetaPerfil)
                .toList();
        return paraPerfil(pessoa, camisetas);
    }

    private PerfilResponseDTO paraPerfil(Pessoa pessoa, List<CamisetaPerfilDTO> camisetas) {
        Nivel nivel = pessoa.getNivel();
        NivelResponseDTO nivelResponse = nivel == null ? null
                : new NivelResponseDTO(nivel.getId(), nivel.getNome(), nivel.getXpMinimo());

        String proximoNivelNome = null;
        Integer xpFaltante = null;
        Integer posicaoRanking = null;
        Integer totalRanking = null;

        if (pessoa.getRole() == Role.PARTICIPANTE && pessoa.getXp() != null) {
            int xp = pessoa.getXp();
            Nivel proximoNivel = nivelRepository
                    .findTopByXpMinimoGreaterThanOrderByXpMinimoAsc(xp)
                    .orElse(null);
            if (proximoNivel != null) {
                proximoNivelNome = proximoNivel.getNome();
                xpFaltante = proximoNivel.getXpMinimo() - xp;
            }
            posicaoRanking = (int) pessoaRepository.countByRoleAndXpGreaterThan(Role.PARTICIPANTE, xp) + 1;
            totalRanking = (int) pessoaRepository.countByRole(Role.PARTICIPANTE);
        }

        return new PerfilResponseDTO(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getEmail(),
                pessoa.getRole() == null ? null : pessoa.getRole().name(),
                pessoa.getRa(),
                camisetas,
                pessoa.getXp(),
                nivelResponse,
                proximoNivelNome,
                xpFaltante,
                posicaoRanking,
                totalRanking
        );
    }

    /* Ranking completo (aba Ranking em /participantes). Só leitura — o id do
       usuário logado (idLogado) entra apenas pra marcar `voce` na resposta,
       nunca é usado pra filtrar ou alterar dado de ninguém. */
    public RankingResponseDTO buscarRanking(Integer idLogado) {
        List<Pessoa> participantes = pessoaRepository.findByRoleAndXpIsNotNullOrderByXpDesc(Role.PARTICIPANTE);

        List<RankingParticipanteDTO> lista = new ArrayList<>();
        for (int i = 0; i < participantes.size(); i++) {
            Pessoa pessoa = participantes.get(i);
            lista.add(new RankingParticipanteDTO(
                    i + 1,
                    pessoa.getNome(),
                    pessoa.getXp(),
                    pessoa.getId().equals(idLogado)
            ));
        }

        String atualizadoEm = LocalTime.now().format(DateTimeFormatter.ofPattern("HH'h'mm"));

        return new RankingResponseDTO(lista, lista.size(), atualizadoEm);
    }

    private ParticipanteResponseDTO paraResposta(Pessoa pessoa) {
        /* Todas as camisetas pedidas: um ingresso pode incluir mais de uma
           grátis/inclusa, e ainda cabem avulsas. */
        List<CamisetaParticipanteDTO> camisetas = pessoa.getCamisaPedidos().stream()
                .map(this::paraCamiseta)
                .toList();
        return paraResposta(pessoa, camisetas);
    }

    /* Mesma resposta, mas com as camisetas informadas explicitamente em vez
       de lidas da coleção lazy da pessoa — necessário logo após um
       replace-all (atualizarCamisetas), quando essa coleção pode não
       refletir ainda o que acabou de ser salvo. */
    private ParticipanteResponseDTO paraResposta(Pessoa pessoa, List<CamisetaParticipanteDTO> camisetas) {
        List<PresencaParticipanteDTO> presencas = pessoa.getEventoParticipantes().stream()
                .map(ep -> new PresencaParticipanteDTO(ep.getStatus().name()))
                .toList();

        // `camiseta` segue sendo a primeira, para as colunas que mostram só uma.
        CamisetaParticipanteDTO camiseta = camisetas.isEmpty() ? null : camisetas.get(0);

        TipoInscricao ingresso = pessoa.getTipoInscricao();
        TipoInscricaoResponseDTO tipoInscricao = ingresso == null ? null
                : new TipoInscricaoResponseDTO(
                        ingresso.getId(), ingresso.getNome(), ingresso.getValor(),
                        ingresso.getAno(), ingresso.getAtivo(), ingresso.getCamisetasGratis(),
                        ingresso.getPorDia(), ingresso.getMaxDias());

        Nivel nivel = pessoa.getNivel();
        NivelResponseDTO nivelResponse = nivel == null ? null
                : new NivelResponseDTO(nivel.getId(), nivel.getNome(), nivel.getXpMinimo());

        PagamentoCartaoInfoDTO pagamentoCartao = pessoa.getFormaPagamento() != FormaPagamento.CARTAO ? null
                : new PagamentoCartaoInfoDTO(
                        pessoa.getMpPaymentId(), pessoa.getMpStatus(), pessoa.getMpStatusDetail(),
                        pessoa.getParcelas(), pessoa.getValorCobrado());

        return new ParticipanteResponseDTO(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getEmail(),
                pessoa.getRa(),
                pessoa.getAtivo(),
                pessoa.getRole() == null ? null : pessoa.getRole().name(),
                camiseta,
                camisetas,
                tipoInscricao,
                pessoa.getDiasInscricao(),
                nivelResponse,
                pessoa.getXp(),
                presencas,
                pessoa.getComprovantePagamento() != null,
                pessoa.getFormaPagamento() == null ? null : pessoa.getFormaPagamento().name(),
                pagamentoCartao
        );
    }

    private CamisetaParticipanteDTO paraCamiseta(CamisaPedido pedido) {
        return new CamisetaParticipanteDTO(pedido.getModelo().name(), pedido.getTamanho().name(), pedido.getAvulsa());
    }

    private CamisetaPerfilDTO paraCamisetaPerfil(CamisaPedido pedido) {
        return new CamisetaPerfilDTO(pedido.getId(), pedido.getModelo().name(), pedido.getTamanho().name(), pedido.getAvulsa());
    }

    /* Substitui a lista inteira de camisetas da pessoa pela enviada
       (replace-all) — editor do /admin, restrito a DIRETOR_SITE/PRESIDENTE
       (ver SecurityConfig). Cobre tanto participante quanto comissão: dá
       pra marcar qualquer camiseta como avulsa, mesmo de alguém da
       comissão, que antes era sempre grátis por regra fixa no código. */
    @Transactional
    public ParticipanteResponseDTO atualizarCamisetas(Integer id, List<CamisetaAdminDTO> camisetas) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Pessoa não encontrada."));

        camisaPedidoRepository.deleteByPessoaId(id);

        List<CamisaPedido> novos = camisetas.stream()
                .map(item -> {
                    CamisaPedido pedido = new CamisaPedido();
                    pedido.setPessoa(pessoa);
                    pedido.setModelo(item.modelo());
                    pedido.setTamanho(item.tamanho());
                    pedido.setAvulsa(item.avulsa());
                    return pedido;
                })
                .toList();
        camisaPedidoRepository.saveAll(novos);

        // Resposta construída a partir da lista recém-salva — a coleção
        // lazy da pessoa pode não refletir o replace-all ainda nesta
        // transação (mesmo cuidado de atualizarPerfil).
        List<CamisetaParticipanteDTO> resposta = novos.stream().map(this::paraCamiseta).toList();
        return paraResposta(pessoa, resposta);
    }

    /* Registra mais uma tentativa de cobrança no cartão (aprovada, recusada
       ou em análise) e devolve o total acumulado. REQUIRES_NEW: precisa
       commitar isolado da transação de PagamentoCartaoService.criarPagamento,
       que é revertida quando a Mercado Pago recusa a chamada em si (erro de
       rede/API) — sem isso, forçar esse tipo de erro deixaria a tentativa
       de fora da contagem usada para travar carding. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int registrarTentativaCartao(Integer pessoaId) {
        Pessoa pessoa = pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inscrição não encontrada."));
        int tentativas = pessoa.getTentativasCartao() + 1;
        pessoa.setTentativasCartao(tentativas);
        pessoaRepository.save(pessoa);
        return tentativas;
    }

    /* Nome do arquivo do comprovante de pagamento da pessoa, salvo em disco
       por InscricaoController no cadastro. Usado por PessoaController para
       servir o arquivo (GET /api/pessoa/{id}/comprovante) — quem confirma a
       inscrição precisa poder ver o que foi enviado. 404 tanto se a pessoa
       não existe quanto se ela não anexou nada. */
    @Transactional(readOnly = true)
    public String buscarNomeComprovante(Integer id) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Pessoa não encontrada."));
        String nome = pessoa.getComprovantePagamento();
        if (nome == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Esta pessoa não anexou comprovante de pagamento.");
        }
        return nome;
    }
}
