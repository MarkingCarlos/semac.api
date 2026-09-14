package com.semac.java_api.service;

import com.semac.java_api.dto.OrcamentoResponseDTO;
import com.semac.java_api.dto.PrevisaoCategoriaResponseDTO;
import com.semac.java_api.dto.PrevisaoItemResponseDTO;
import com.semac.java_api.dto.PrevisaoResumoDTO;
import com.semac.java_api.model.*;
import com.semac.java_api.model.enums.ContaFinanceira;
import com.semac.java_api.model.enums.StatusCompra;
import com.semac.java_api.model.enums.StatusPagamento;
import com.semac.java_api.model.enums.StatusPrevisao;
import com.semac.java_api.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
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

    public PrevisaoService(PrevisaoItemRepository itemRepository,
                           PrevisaoCategoriaRepository categoriaRepository,
                           OrcamentoRepository orcamentoRepository,
                           CompraRepository compraRepository,
                           PatrocinadorRepository patrocinadorRepository,
                           DoadorRepository doadorRepository,
                           CaixaRepository caixaRepository) {
        this.itemRepository = itemRepository;
        this.categoriaRepository = categoriaRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.compraRepository = compraRepository;
        this.patrocinadorRepository = patrocinadorRepository;
        this.doadorRepository = doadorRepository;
        this.caixaRepository = caixaRepository;
    }

    /* ── Escala ──────────────────────────────────────────────────── */

    /* Quanto o valor de um item se multiplica. Orçamento ausente devolve
       fator 1 em vez de estourar: sem orçamento cadastrado a previsão
       ainda deve ser legível, só não escala. */
    public int fator(PrevisaoItem item, Orcamento orcamento) {
        if (orcamento == null || item.getEscala() == null) return 1;
        return switch (item.getEscala()) {
            case FIXA -> 1;
            case POR_INSCRITO -> orcamento.getInscritosPrevistos();
            case POR_COMISSAO -> orcamento.getMembrosComissao();
            case POR_PALESTRANTE -> orcamento.getPalestrantesPrevistos();
        };
    }

    /* (valorUnitario × quantidade + frete) × fator da escala. */
    public BigDecimal valorTotal(PrevisaoItem item, Orcamento orcamento) {
        BigDecimal frete = item.getFrete() == null ? BigDecimal.ZERO : item.getFrete();
        BigDecimal base = item.getValorUnitario()
                .multiply(BigDecimal.valueOf(item.getQuantidade()))
                .add(frete);
        return base.multiply(BigDecimal.valueOf(fator(item, orcamento)));
    }

    public Orcamento orcamentoVigente() {
        return orcamentoRepository.findFirstByOrderByAnoDesc().orElse(null);
    }

    /* ── Leitura ─────────────────────────────────────────────────── */

    public List<PrevisaoItemResponseDTO> listarItens() {
        Orcamento orcamento = orcamentoVigente();
        return itemRepository.findAllByOrderByCategoria_OrdemAscIdAsc().stream()
                .map(item -> paraResposta(item, orcamento))
                .toList();
    }

    public PrevisaoItemResponseDTO paraResposta(PrevisaoItem item, Orcamento orcamento) {
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
                fator(item, orcamento),
                valorTotal(item, orcamento),
                item.getConta() == null ? null : item.getConta().name(),
                item.getStatus().name(),
                item.getDataPrevista(),
                item.getObservacao(),
                item.getCompra() == null ? null : item.getCompra().getId()
        );
    }

    /* ── Consolidado do dashboard ────────────────────────────────── */

    public PrevisaoResumoDTO resumo() {
        Orcamento orcamento = orcamentoVigente();
        List<PrevisaoItem> itens = itemRepository.findAll();
        List<Compra> compras = compraRepository.findAll();

        /* Um item PAGO já virou compra: sai da previsão para não ser
           contado duas vezes. */
        Predicate<PrevisaoItem> emAberto = item -> item.getStatus() != StatusPrevisao.PAGO;

        BigDecimal previstoAberto = itens.stream()
                .filter(emAberto)
                .map(item -> valorTotal(item, orcamento))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal realizado = compras.stream()
                .map(Compra::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal projecaoTotal = previstoAberto.add(realizado);
        BigDecimal teto = orcamento == null ? BigDecimal.ZERO : orcamento.getTeto();

        /* ── Por categoria ── */
        Map<Integer, BigDecimal> previstoPorCategoria = itens.stream()
                .filter(emAberto)
                .collect(Collectors.groupingBy(
                        item -> item.getCategoria().getId(),
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> valorTotal(item, orcamento), BigDecimal::add)));

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

        /* ── Por conta ── */
        List<PrevisaoResumoDTO.ContaResumoDTO> contas = new ArrayList<>();
        for (ContaFinanceira conta : ContaFinanceira.values()) {
            BigDecimal caixaInicial = caixaRepository.findByConta(conta)
                    .map(Caixa::getValor)
                    .orElse(BigDecimal.ZERO);

            /* Só patrocínio efetivamente recebido entra como entrada —
               a planilha somava também o que ainda estava pendente. */
            BigDecimal entradas = patrocinadorRepository.findAll().stream()
                    .filter(p -> p.getConta() == conta)
                    .filter(p -> p.getStatusPagamento() == StatusPagamento.RECEBIDO)
                    .map(Patrocinador::getValorFinal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(doadorRepository.findAll().stream()
                            .filter(d -> d.getConta() == conta)
                            .map(Doador::getValor)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));

            BigDecimal saidas = compras.stream()
                    .filter(c -> c.getConta() == conta)
                    .filter(c -> c.getStatus() == StatusCompra.PAGO)
                    .map(Compra::getValorTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal previsto = itens.stream()
                    .filter(emAberto)
                    .filter(item -> item.getConta() == conta)
                    .map(item -> valorTotal(item, orcamento))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            contas.add(new PrevisaoResumoDTO.ContaResumoDTO(
                    conta.name(),
                    caixaInicial,
                    entradas,
                    saidas,
                    previsto,
                    caixaInicial.add(entradas).subtract(saidas)));
        }

        return new PrevisaoResumoDTO(
                previstoAberto,
                realizado,
                projecaoTotal,
                teto,
                teto.subtract(projecaoTotal),
                categorias,
                contas,
                orcamento == null ? null : new OrcamentoResponseDTO(
                        orcamento.getId(),
                        orcamento.getAno(),
                        orcamento.getTeto(),
                        orcamento.getInscritosPrevistos(),
                        orcamento.getMembrosComissao(),
                        orcamento.getPalestrantesPrevistos()));
    }
}
