package com.semac.java_api.dto;

import com.semac.java_api.model.enums.StatusPagamento;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusPagamentoDTO(
        @NotNull StatusPagamento statusPagamento
) {}
