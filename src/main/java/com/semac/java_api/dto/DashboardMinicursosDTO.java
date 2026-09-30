package com.semac.java_api.dto;

import java.util.List;

/* Card "Vagas restantes em minicursos" da dashboard: os totais somados
   de todos os minicursos (o que o card mostra) e cada um em detalhe (o
   que o modal lista). */
public record DashboardMinicursosDTO(
        int capacidadeTotal,
        int inscritosTotal,
        int vagasRestantesTotal,
        List<DashboardMinicursoDTO> minicursos
) {}
