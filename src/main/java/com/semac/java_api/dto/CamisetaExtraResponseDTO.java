package com.semac.java_api.dto;

import java.math.BigDecimal;

/* Preço da camiseta avulsa da edição. `id` vem null enquanto o preço não
   foi cadastrado — nesse caso `valor` é zero. */
public record CamisetaExtraResponseDTO(
        Integer id,
        Integer ano,
        BigDecimal valor
) {}
