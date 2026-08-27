package com.semac.java_api.dto;

import java.math.BigDecimal;

/* Meta de arrecadacao da edicao. `id` vem null enquanto a meta não foi
   cadastrada — nesse caso `valor` é zero. */
public record MetaDoacaoResponseDTO(
        Integer id,
        Integer ano,
        BigDecimal valor
) {}
