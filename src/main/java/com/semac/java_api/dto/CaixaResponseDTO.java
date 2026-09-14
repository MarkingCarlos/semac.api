package com.semac.java_api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CaixaResponseDTO(
        Integer id,
        BigDecimal valor,
        String conta,
        LocalDateTime dataAtualizacao,
        Integer atualizadoPorId,
        String atualizadoPorNome
) {}
