package com.semac.java_api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resumo exibido na página pública de doações. Não expõe valores
 * individuais nem datas — apenas o total arrecadado e os nomes dos
 * doadores (deduplicados, da doação mais recente para a mais antiga).
 */
public record ResumoDoacaoPublicoDTO(
        BigDecimal totalArrecadado,
        List<String> doadores
) {}
