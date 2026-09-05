package com.semac.java_api.dto;

import java.math.BigDecimal;

public record PagamentoCartaoResponseDTO(
        Long mpPaymentId,
        String status,
        String statusDetail,
        Integer parcelas,
        BigDecimal valorCobrado
) {}
