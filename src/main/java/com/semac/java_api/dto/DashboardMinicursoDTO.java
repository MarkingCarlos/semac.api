package com.semac.java_api.dto;

import java.time.LocalDateTime;

/* Um minicurso no modal de vagas da dashboard do /admin. `inscritos`
   conta quem ocupa vaga (INSCRITO ou PRESENTE — mesma regra da inscrição,
   ver EventoParticipanteRepository.countByPk_EventoIdAndStatusIn). */
public record DashboardMinicursoDTO(
        Integer id,
        String nome,
        LocalDateTime dataHoraInicio,
        String local,
        int capacidade,
        int inscritos,
        int vagasRestantes
) {}
