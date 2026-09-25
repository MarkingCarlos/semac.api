package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/* Conjunto completo de dias escolhidos (substitui a escolha anterior). */
public record EscolherDiasIngressoRequestDTO(
        @NotNull List<LocalDate> dias
) {}
