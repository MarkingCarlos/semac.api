package com.semac.java_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

/* `local` é opcional; `palestrantes` pode vir vazio (ex: coffee break).
   dataHoraInicio/dataHoraFim chegam combinados (data + hora) pela borda
   da API no frontend. */
public record EventoRequestDTO(
        @NotBlank String nome,
        @NotNull Integer tipoEventoId,
        String local,
        String descricao,
        @NotNull LocalDateTime dataHoraInicio,
        @NotNull LocalDateTime dataHoraFim,
        @NotNull @Positive Integer capacidadeMaxima,
        @Valid List<PalestranteDTO> palestrantes
) {}
