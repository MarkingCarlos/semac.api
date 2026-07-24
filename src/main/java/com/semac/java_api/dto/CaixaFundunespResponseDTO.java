package com.semac.java_api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CaixaFundunespResponseDTO(
        Integer id,
        BigDecimal valor,
        LocalDateTime dataAtualizacao,
        Integer atualizadoPorId,
        String atualizadoPorNome
) {}
