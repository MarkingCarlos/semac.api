package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/* A data de um dos quatro dias. É ela que decide qual dia está valendo. */
public record TermoDiaRequestDTO(
        @NotNull Integer ano,
        @NotNull LocalDate data
) {}
