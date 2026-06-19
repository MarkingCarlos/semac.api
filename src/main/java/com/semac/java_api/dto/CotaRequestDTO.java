package com.semac.java_api.dto;

import com.semac.java_api.model.enums.NivelPatrocinio;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CotaRequestDTO(
        @NotNull NivelPatrocinio nivel,
        @NotNull @PositiveOrZero BigDecimal valor
) {}
