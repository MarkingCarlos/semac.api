package com.semac.java_api.dto;

import java.time.LocalDateTime;
import java.util.List;

/* `vagasRestantes` só faz sentido em evento que exige inscrição
   (minicurso) — vem null nos eventos abertos, cuja capacidade é folgada
   e em que todo participante confirmado já entra automaticamente. */
public record EventoResponseDTO(
        Integer id,
        String nome,
        TipoEventoResponseDTO tipoEvento,
        String local,
        String descricao,
        TrilhaResponseDTO trilha,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        Integer capacidadeMaxima,
        Integer vagasRestantes,
        List<PalestranteDTO> palestrantes
) {}
