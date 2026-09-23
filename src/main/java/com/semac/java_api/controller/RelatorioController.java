package com.semac.java_api.controller;

import com.semac.java_api.dto.RelatorioCamisetasComissaoDTO;
import com.semac.java_api.dto.RelatorioCamisetasDTO;
import com.semac.java_api.service.RelatorioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relatorio")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    /* Relatório de camisetas da aba "Relatórios" do /admin. */
    @GetMapping("/camisetas")
    public RelatorioCamisetasDTO relatorioCamisetas() {
        return relatorioService.relatorioCamisetas();
    }

    /* Relatório só das camisetas da comissão (inclusas no kit, sem avulsas). */
    @GetMapping("/camisetas-comissao")
    public RelatorioCamisetasComissaoDTO relatorioCamisetasComissao() {
        return relatorioService.relatorioCamisetasComissao();
    }
}
