package com.semac.java_api.dto;

import java.time.LocalDateTime;

/* Resumo de leitura de QR de um evento. `esperados` é quem tem vaga ou
   pré-inscrição no evento (qualquer status); `leituras` é quantos QR
   foram lidos (status PRESENTE). */
public record DashboardEventoCheckinDTO(
        Integer id,
        String nome,
        String tipo,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        int esperados,
        int leituras
) {}
