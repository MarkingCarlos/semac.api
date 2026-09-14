package com.semac.java_api.service;

import com.semac.java_api.dto.OrcamentoResponseDTO;
import com.semac.java_api.dto.PrevisaoCategoriaResponseDTO;
import com.semac.java_api.dto.PrevisaoItemResponseDTO;
import com.semac.java_api.dto.InscricaoFinanceiraDTO;
import com.semac.java_api.dto.PrevisaoResumoDTO;
import com.semac.java_api.model.*;
import com.semac.java_api.model.enums.ContaFinanceira;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.model.enums.StatusPagamento;
import com.semac.java_api.model.enums.StatusPrevisao;
import com.semac.java_api.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/* Regras de cálculo da previsão de gastos.

   Concentra dois pontos que não deveriam ficar espalhados:

   1. A escala. O total de um item previsto não é persistido — depende de
      quantos inscritos/membros/palestrantes o orçamento estima no
      momento da leitura. É aqui que o fator é aplicado.

   2. A separação entre previsto e realizado. Um item PAGO já virou
      `compra` e conta como realizado; contá-lo também como previsão
      duplicaria o gasto. Foi exatamente esse tipo de dupla contagem que
      deixou o balanço da planilha de origem inconsistente. */
@Service
public class PrevisaoService {

    private final PrevisaoItemRepository itemRepository;
    private final PrevisaoCategoriaRepository categoriaRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final CompraRepository compraRepository;
    private final PatrocinadorRepository patrocinadorRepository;
    private final DoadorRepository doadorRepository;
    private final CaixaRepository caixaRepository;
    private final PessoaService pessoaService;
    private final PessoaRepository pessoaRepository;

    public PrevisaoService(PrevisaoItemRepository itemRepository,
                           PrevisaoCategoriaRepository categoriaRepository,
                           OrcamentoRepository orcamentoRepository,
                           CompraRepository compraRepository,
                           PatrocinadorRepository patrocinadorRepository,
                           DoadorRepository doadorRepository,
                           CaixaRepository caixaRepository,
                           PessoaService pessoaService,
                           PessoaRepository pessoaRepository) {
        this.itemRepository = itemRepository;
        this.categoriaRepository = categoriaRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.compraRepository = compraRepository;
        this.patrocinadorRepository = patrocinadorRepository;
        this.doadorRepository = doadorRepository;
        this.caixaRepository = caixaRepository;
        this.pessoaService = pessoaService;
        this.pessoaRepository = pessoaRepository;
    }

    /* ── Escala ──────────────────────────────────────────────────── */

    /* Os multiplicadores das escalas, lidos de uma vez.

       `inscritos` é DERIVADO: a contagem de pessoas com role PARTICIPANTE
       (confirmadas) ou NULL (aguardando confirmação) — as mesmas que o
       /admin lista em "Participantes". Digitado, esse número virava uma
       armadilha silenciosa: bastava ficar zerado para todo item por
       inscrito valer R$ 0,00 sem explicação.

       Os outros dois seguem vindo do orçamento. */
    public record Fatores(int inscritos, int comissao, int palestrantes) {}

    public Fatores fatoresVigentes() {
        Orcamento orcamento = orcamentoVigente();
        int inscritos = (int) pessoaRepository.countByRoleIsNullOrRole(Role.PARTICIPANTE);
        return new Fatores(
                inscritos,
                orcamento == null ? 0 : orcamento.getMembrosComissao(),
                orcamento == null ? 0 : orcamento.getPalestrantesPrevistos());
    }

    /* Quanto o valor de um item se multiplica. */
    public int fator(PrevisaoItem item, Fatores fatores) {
        if (item.getEscala() == null) return 1;
        return switch (item.getEscala()) {
            case FIXA -> 1;
            case POR_INSCRITO -> fatores.inscritos();
            case POR_COMISSAO -> fatores.comissao();
            case POR_PALESTRANTE -> fatores.palestrantes();
        };
    }

    /* (valorUnitario × quantidade + frete) × fator da escala. */
    public BigDecimal valorTotal(PrevisaoItem item, Fatores fatores) {
        BigDecimal frete = item.getFrete() == null ? BigDecimal.ZERO : item.getFrete();
        BigDecimal base = item.getValorUnitario()
                .multiply(BigDecimal.valueOf(item.getQuantidade()))
                .add(frete);
        return base.multiply(BigDecimal.valueOf(fator(item, fatores)));
    }

    public Orcamento orcamentoVigente() {
        return orcamentoRepository.findFirstByOrderByAnoDesc().orElse(null);
    }

    /* ── Leitura ─────────────────────────────────────────────────── */

    public List<PrevisaoItemResponseDTO> listarItens() {
        Fatores fatores = fatoresVigentes();
        return itemRepository.findAllByOrderByCategoria_OrdemAscIdAsc().stream()
                .map(item -> paraResposta(item, fatores))
                .toList();
    }

    public PrevisaoItemResponseDTO paraResposta(PrevisaoItem item, Fatores fatores) {
        PrevisaoCategoria categoria = item.getCategoria();
        Fornecedor fornecedor = item.getFornecedor();
        return new PrevisaoItemResponseDTO(
                item.getId(),
                item.getDescricao(),
                categoria.getId(),
                categoria.getNome(),
                categoria.getCor(),
                fornecedor == null ? null : fornecedor.getId(),
                fornecedor == null ? null : fornecedor.getNome(),
                item.getQuantidade(),
                item.getValorUnitario(),
                item.getFrete(),
                item.getEscala().name(),
                fator(item, fatores),
                valorTotal(item, fatores),
                item.getStatus().name(),
                item.getDataPrevista(),
                item.getObservacao(),
                item.getCompra() == null ? null : item.getCompra().getId()
        );
    }

    /* ── Consolidado do dashboard ────────────────────────────────── */

    public PrevisaoResumoDTO resumo() {
        Orcamento orcamento = orcamentoVigente();
        Fatores fatores = fatoresVigentes();
        List<PrevisaoItem> itens = itemRepository.findAll();
        List<Compra> compras = compraRepository.findAll();

        /* Um item PAGO já virou compra: sai da previsão para não ser
           contado duas vezes. */
        Predicate<PrevisaoItem> emAberto = item -> item.getStatus() != StatusPrevisao.PAGO;

        BigDecimal previstoAberto = itens.stream()
                .filter(emAberto)
                .map(item -> valorTotal(item, fatores))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal realizado = compras.stream()
                .map(Compra::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal projecaoTotal = previstoAberto.add(realizado);

        /* ── Por categoria ── */
        Map<Integer, BigDecimal> previstoPorCategoria = itens.stream()
                .filter(emAberto)
                .collect(Collectors.groupingBy(
                        item -> item.getCategoria().getId(),
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> valorTotal(item, fatores), BigDecimal::add)));

        /* Compras não têm categoria_id: `compra.categoria` é texto livre.
           O casamento é pelo nome da categoria, e o que não casar cai
           fora do gráfico por categoria (mas segue no total realizado). */
        Map<String, BigDecimal> realizadoPorNomeCategoria = compras.stream()
                .filter(compra -> compra.getCategoria() != null)
                .collect(Collectors.groupingBy(
                        compra -> compra.getCategoria().trim().toLowerCase(),
                        Collectors.reducing(BigDecimal.ZERO,
                                Compra::getValorTotal, BigDecimal::add)));

        List<PrevisaoCategoriaResponseDTO> categorias =
                categoriaRepository.findAllByOrderByOrdemAscNomeAsc().stream()
                        .map(categoria -> new PrevisaoCategoriaResponseDTO(
                                categoria.getId(),
                                categoria.getNome(),
                                categoria.getCor(),
                                categoria.getTeto(),
                                categoria.getOrdem(),
                                previstoPorCategoria.getOrDefault(categoria.getId(), BigDecimal.ZERO),
                                realizadoPorNomeCategoria.getOrDefault(
                                        categoria.getNome().trim().toLowerCase(), BigDecimal.ZERO)))
                        .toList();

        /* ── O que a comissão arrecadou ──
           É isto que a comissão tem para gastar, e por isso é o teto da
           previsão. Não desconta compras: elas já entram na projeção, e
           descontá-las aqui as contaria duas vezes.

           Não há filtro por conta. A FUNDUNESP é reserva de emergência —
           não recebe entrada nem paga saída —, então não existe
           lançamento a atribuir a ela e todo dinheiro que entra é da
           comissão. As inscrições nunca tiveram conta própria (nem
           `pessoa` nem `tipo_inscricao` têm o campo); o valor vem de
           PessoaService para não reimplementar a regra de ingresso por
           diária (valor × dias). */
        BigDecimal totalPatrocinios = patrocinadorRepository.findAll().stream()
                .filter(p -> p.getStatusPagamento() == StatusPagamento.RECEBIDO)
                .map(Patrocinador::getValorFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDoacoes = doadorRepository.findAll().stream()
                .map(Doador::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalInscricoes = pessoaService.listarInscricoes().stream()
                .map(InscricaoFinanceiraDTO::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal teto = totalPatrocinios.add(totalDoacoes).add(totalInscricoes);

        PrevisaoResumoDTO.EntradasDTO entradas = new PrevisaoResumoDTO.EntradasDTO(
                totalPatrocinios, totalDoacoes, totalInscricoes, teto);

        /* Reserva de emergência: valor digitado, exibido à parte. Não
           entra no teto nem em nenhum cálculo. */
        BigDecimal reservaFundunesp = caixaRepository.findByConta(ContaFinanceira.FUNDUNESP)
                .map(Caixa::getValor)
                .orElse(BigDecimal.ZERO);

        return new PrevisaoResumoDTO(
                previstoAberto,
                realizado,
                projecaoTotal,
                teto,
                teto.subtract(projecaoTotal),
                entradas,
                reservaFundunesp,
                categorias,
                orcamento == null ? null : new OrcamentoResponseDTO(
                        orcamento.getId(),
                        orcamento.getAno(),
                        fatores.inscritos(),
                        orcamento.getMembrosComissao(),
                        orcamento.getPalestrantesPrevistos()));
    }
}
