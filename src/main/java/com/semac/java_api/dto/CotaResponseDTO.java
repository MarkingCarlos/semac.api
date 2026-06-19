package com.semac.java_api.dto;

import com.semac.java_api.model.enums.NivelPatrocinio;

import java.math.BigDecimal;

public record CotaResponseDTO(
        Integer id,
        NivelPatrocinio nivel,
        BigDecimal valor
) {}
