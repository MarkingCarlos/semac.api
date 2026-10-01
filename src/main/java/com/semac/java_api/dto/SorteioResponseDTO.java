package com.semac.java_api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SorteioResponseDTO(
        Integer id,
        String nome,
        Integer eventoId,
        String eventoNome,
        LocalDateTime eventoDataHoraInicio,
        Integer quantidadeBrindes,
        Integer quantidadeEntregue,
        List<GanhadorSorteioDTO> ganhadores
) {}
