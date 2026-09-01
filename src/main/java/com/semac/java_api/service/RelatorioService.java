package com.semac.java_api.service;

import com.semac.java_api.dto.ItemEstoqueCamisetaDTO;
import com.semac.java_api.dto.RelatorioCamisetasDTO;
import com.semac.java_api.model.CamisetaExtra;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.repository.CamisaPedidoRepository;
import com.semac.java_api.repository.CamisetaExtraRepository;
import com.semac.java_api.repository.projection.ContagemCamisetaGrupoView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

/* Relatórios gerenciais do /admin (aba "Relatórios"). Cada relatório vive
   num método próprio aqui — hoje só o de camisetas. */
@Service
public class RelatorioService {

    /* Custo de produção de cada camiseta avulsa. Diferente do preço de
       venda (tabela camiseta_extra, editável em Informações SEMAC), o
       custo ainda não tem tela própria — atualizar aqui quando o
       fornecedor mudar o valor. */
    private static final BigDecimal CUSTO_CAMISETA_AVULSA = new BigDecimal("44.00");

    private final CamisaPedidoRepository camisaPedidoRepository;
    private final CamisetaExtraRepository camisetaExtraRepository;

    public RelatorioService(CamisaPedidoRepository camisaPedidoRepository,
                             CamisetaExtraRepository camisetaExtraRepository) {
        this.camisaPedidoRepository = camisaPedidoRepository;
        this.camisetaExtraRepository = camisetaExtraRepository;
    }

    /* Camisetas a comprar: soma de tudo que já foi pedido no cadastro
       (participantes e comissão), dividido entre "dadas" (inclusas no
       ingresso/kit) e "avulsas" (compra à parte), e entre "comissão" e
       "participantes" pelo role da pessoa. Dadas/avulsas vem direto do
       campo `avulsa` de cada CamisaPedido — editável no /admin por
       DIRETOR_SITE/PRESIDENTE (ver PessoaService.atualizarCamisetas), não é
       mais calculado comparando com o ingresso.

       O financeiro (receita/custo/lucro) considera só as avulsas — as
       dadas já estão cobertas pelo preço do ingresso. Receita usa o preço
       vigente em camiseta_extra para o ano corrente; custo usa a
       constante acima. */
    @Transactional(readOnly = true)
    public RelatorioCamisetasDTO relatorioCamisetas() {
        int totalGeral = 0;
        int totalDadas = 0;
        int totalComissao = 0;
        int totalParticipantes = 0;

        for (ContagemCamisetaGrupoView linha : camisaPedidoRepository.contarCamisetasPorAvulsaERole()) {
            int total = linha.getTotal().intValue();
            totalGeral += total;

            if (!Boolean.TRUE.equals(linha.getAvulsa())) {
                totalDadas += total;
            }

            if (linha.getRole() == Role.PARTICIPANTE) {
                totalParticipantes += total;
            } else {
                totalComissao += total;
            }
        }

        int totalAvulsas = totalGeral - totalDadas;

        BigDecimal precoAvulsa = camisetaExtraRepository.findByAno(Year.now().getValue())
                .map(CamisetaExtra::getValor)
                .orElse(BigDecimal.ZERO);
        BigDecimal quantidadeAvulsas = BigDecimal.valueOf(totalAvulsas);
        BigDecimal receitaAvulsas = precoAvulsa.multiply(quantidadeAvulsas);
        BigDecimal custoAvulsas = CUSTO_CAMISETA_AVULSA.multiply(quantidadeAvulsas);
        BigDecimal lucroAvulsas = receitaAvulsas.subtract(custoAvulsas);

        List<ItemEstoqueCamisetaDTO> porModeloTamanho = camisaPedidoRepository.consultarEstoque().stream()
                .map(v -> new ItemEstoqueCamisetaDTO(v.getModelo().name(), v.getTamanho().name(), v.getTotal()))
                .toList();

        return new RelatorioCamisetasDTO(
                totalGeral, totalDadas, totalAvulsas,
                totalComissao, totalParticipantes,
                receitaAvulsas, custoAvulsas, lucroAvulsas,
                porModeloTamanho
        );
    }
}
