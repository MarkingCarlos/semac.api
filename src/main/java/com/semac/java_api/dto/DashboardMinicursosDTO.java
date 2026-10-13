package com.semac.java_api.dto;

import java.util.List;

/* Card "Vagas restantes em minicursos" da dashboard: os totais somados
   de todos os minicursos (o que o card mostra) e cada um em detalhe (o
   que o modal lista).

   participantesTotal / participantesSemMinicurso: participantes
   confirmados que podem fazer minicurso (diaristas só contam se algum
   dia escolhido tem minicurso) e, deles, quantos ainda não ocupam vaga
   em nenhum. */
public record DashboardMinicursosDTO(
        int capacidadeTotal,
        int inscritosTotal,
        int vagasRestantesTotal,
        int participantesTotal,
        int participantesSemMinicurso,
        List<DashboardMinicursoDTO> minicursos
) {}
