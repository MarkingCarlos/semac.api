package com.semac.java_api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DoadorResponseDTO(
        Integer id,
        String nome,
        BigDecimal valor,
        LocalDateTime data
) {}
