package com.semac.java_api.service;

import com.semac.java_api.dto.ItemEstoqueCamisetaDTO;
import com.semac.java_api.dto.RelatorioCamisetasDTO;
import com.semac.java_api.repository.CamisaPedidoRepository;
import com.semac.java_api.repository.projection.ContagemCamisetaPessoaView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/* Relatórios gerenciais do /admin (aba "Relatórios"). Cada relatório vive
   num método próprio aqui — hoje só o de camisetas. */
@Service
public class RelatorioService {

    private final CamisaPedidoRepository camisaPedidoRepository;

    public RelatorioService(CamisaPedidoRepository camisaPedidoRepository) {
        this.camisaPedidoRepository = camisaPedidoRepository;
    }

    /* Camisetas a comprar: soma de tudo que já foi pedido no cadastro
       (participantes e comissão), dividido entre "dadas" (inclusas no
       ingresso) e "avulsas" (excedente, compra à parte). A divisão é por
       pessoa — compara o total que ela pediu com o camisetasGratis do
       ingresso vinculado. Quem é confirmado como comissão perde esse
       vínculo (PessoaService.atribuirRole zera o tipoInscricao), e como
       ninguém da comissão paga por camiseta no sistema, tudo que ela já
       pediu conta como dado. */
    @Transactional(readOnly = true)
    public RelatorioCamisetasDTO relatorioCamisetas() {
        int totalGeral = 0;
        int totalDadas = 0;

        for (ContagemCamisetaPessoaView linha : camisaPedidoRepository.contarCamisetasPorPessoa()) {
            int total = linha.getTotalCamisetas().intValue();
            int gratisPermitidas = linha.getCamisetasGratis() != null ? linha.getCamisetasGratis() : total;
            totalGeral += total;
            totalDadas += Math.min(total, gratisPermitidas);
        }

        List<ItemEstoqueCamisetaDTO> porModeloTamanho = camisaPedidoRepository.consultarEstoque().stream()
                .map(v -> new ItemEstoqueCamisetaDTO(v.getModelo().name(), v.getTamanho().name(), v.getTotal()))
                .toList();

        return new RelatorioCamisetasDTO(totalGeral, totalDadas, totalGeral - totalDadas, porModeloTamanho);
    }
}
