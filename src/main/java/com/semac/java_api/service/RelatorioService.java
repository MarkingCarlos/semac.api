package com.semac.java_api.service;

import com.semac.java_api.dto.ItemEstoqueCamisetaDTO;
import com.semac.java_api.dto.RelatorioCamisetasComissaoDTO;
import com.semac.java_api.dto.RelatorioCamisetasDTO;
import com.semac.java_api.dto.RelatorioCamisetasParticipantesDTO;
import com.semac.java_api.model.CamisetaExtra;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.repository.CamisaPedidoRepository;
import com.semac.java_api.repository.CamisetaExtraRepository;
import com.semac.java_api.repository.projection.ContagemCamisetaGrupoView;
import com.semac.java_api.repository.projection.EstoqueView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

/* Relatórios gerenciais do /admin (aba "Relatórios"). Cada relatório vive
   num método próprio aqui — hoje camisetas (geral, da comissão e de participantes). */
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
       ingresso/kit) e "avulsas" (compra à parte). Dadas/avulsas vem direto
       do campo `avulsa` de cada CamisaPedido — editável no /admin por
       DIRETOR_SITE/PRESIDENTE (ver PessoaService.atualizarCamisetas), não é
       mais calculado comparando com o ingresso.

       Entram também os inscritos ainda não confirmados no /admin (role
       null): eles já pediram camiseta no cadastro e o relatório existe para
       fechar a compra com o fornecedor. Pendente conta como participante —
       é o que ele será ao confirmar, e ninguém entra na comissão pelo
       cadastro público.

       "Comissão" × "participantes" (seção Por perfil) não é simplesmente o
       role de quem pediu: toda avulsa é do modelo de participante, mesmo
       quando quem compra é da comissão — a camiseta exclusiva da comissão é
       só a inclusa no kit. Por isso só as dadas entram pelo role; toda
       avulsa cai em "participantes".

       O financeiro (receita/custo/lucro) considera só as avulsas — as
       dadas já estão cobertas pelo preço do ingresso —, inclusive as de
       pendentes, então a receita é projeção: parte pode ainda não ter sido
       paga. Receita usa o preço vigente em camiseta_extra para o ano
       corrente; custo usa a constante acima. */
    @Transactional(readOnly = true)
    public RelatorioCamisetasDTO relatorioCamisetas() {
        int totalGeral = 0;
        int totalDadas = 0;
        int totalComissao = 0;
        int totalParticipantes = 0;

        for (ContagemCamisetaGrupoView linha : camisaPedidoRepository.contarCamisetasPorAvulsaERole()) {
            int total = linha.getTotal().intValue();
            totalGeral += total;

            if (Boolean.TRUE.equals(linha.getAvulsa())) {
                totalParticipantes += total;
            } else {
                totalDadas += total;
                if (linha.getRole() != null && linha.getRole() != Role.PARTICIPANTE) {
                    totalComissao += total;
                } else {
                    totalParticipantes += total;
                }
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

        List<ItemEstoqueCamisetaDTO> porModeloTamanho = paraItensEstoque(camisaPedidoRepository.consultarEstoque());

        return new RelatorioCamisetasDTO(
                totalGeral, totalDadas, totalAvulsas,
                totalComissao, totalParticipantes,
                receitaAvulsas, custoAvulsas, lucroAvulsas,
                porModeloTamanho
        );
    }

    /* Camisetas exclusivas da comissão: só as inclusas no kit de quem tem
       role de comissão. Avulsas ficam de fora mesmo quando compradas pela
       comissão, porque são do modelo de participante (mesma regra do
       totalComissao em relatorioCamisetas). */
    @Transactional(readOnly = true)
    public RelatorioCamisetasComissaoDTO relatorioCamisetasComissao() {
        List<ItemEstoqueCamisetaDTO> porModeloTamanho = paraItensEstoque(camisaPedidoRepository.consultarEstoqueComissao());
        return new RelatorioCamisetasComissaoDTO(somarTotal(porModeloTamanho), porModeloTamanho);
    }

    /* Camisetas do modelo de participante: inclusas no kit de participante,
       de pendentes (que contam como participante) e todas as avulsas,
       inclusive as compradas pela comissão (mesma regra do
       totalParticipantes em relatorioCamisetas). */
    @Transactional(readOnly = true)
    public RelatorioCamisetasParticipantesDTO relatorioCamisetasParticipantes() {
        List<ItemEstoqueCamisetaDTO> porModeloTamanho = paraItensEstoque(camisaPedidoRepository.consultarEstoqueParticipantes());
        return new RelatorioCamisetasParticipantesDTO(somarTotal(porModeloTamanho), porModeloTamanho);
    }

    private static List<ItemEstoqueCamisetaDTO> paraItensEstoque(List<EstoqueView> linhas) {
        return linhas.stream()
                .map(v -> new ItemEstoqueCamisetaDTO(v.getModelo().name(), v.getTamanho().name(), v.getTotal()))
                .toList();
    }

    private static int somarTotal(List<ItemEstoqueCamisetaDTO> itens) {
        return itens.stream().mapToInt(item -> (int) item.total()).sum();
    }
}
