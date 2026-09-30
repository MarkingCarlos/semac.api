package com.semac.java_api.controller;

import com.semac.java_api.dto.DashboardCheckinsDTO;
import com.semac.java_api.dto.DashboardDetalheCheckinDTO;
import com.semac.java_api.dto.DashboardInscritoMinicursoDTO;
import com.semac.java_api.dto.DashboardMinicursosDTO;
import com.semac.java_api.dto.DashboardRankingDTO;
import com.semac.java_api.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/* Dashboard da seção Início do /admin (cards com modal de detalhe).
   Acesso restrito em SecurityConfig. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /* Card de vagas: totais de todos os minicursos + cada um em detalhe. */
    @GetMapping("/minicursos")
    public DashboardMinicursosDTO minicursos() {
        return dashboardService.minicursos();
    }

    /* Quem ocupa vaga num minicurso (ao clicar nele no modal). */
    @GetMapping("/minicursos/{id}/inscritos")
    public List<DashboardInscritoMinicursoDTO> inscritosDoMinicurso(@PathVariable Integer id) {
        return dashboardService.inscritosDoMinicurso(id);
    }

    /* Ranking completo por xp (o card mostra o top 3). */
    @GetMapping("/ranking")
    public List<DashboardRankingDTO> ranking() {
        return dashboardService.ranking();
    }

    /* Leituras de QR: eventos com check-in aberto agora, eventos já
       abertos e total por membro. O front consulta em intervalos curtos. */
    @GetMapping("/checkins")
    public DashboardCheckinsDTO checkins() {
        return dashboardService.checkins();
    }

    /* Quem leu o QR de quem num evento específico. */
    @GetMapping("/checkins/eventos/{id}")
    public DashboardDetalheCheckinDTO detalheCheckin(@PathVariable Integer id) {
        return dashboardService.detalheCheckin(id);
    }
}
