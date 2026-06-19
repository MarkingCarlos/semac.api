package com.semac.java_api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EventoResponseDTO(
        Integer id,
        String nome,
        TipoEventoResponseDTO tipoEvento,
        String local,
        String descricao,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        Integer capacidadeMaxima,
        List<PalestranteDTO> palestrantes
) {}
