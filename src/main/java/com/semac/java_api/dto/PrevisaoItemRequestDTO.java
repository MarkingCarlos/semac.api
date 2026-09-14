package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/* valorTotal não vem do cliente: depende da escala e do orçamento
   vigente, e é calculado pelo backend na leitura.

   quantidade aceita 0 — item com preço já pesquisado e quantidade ainda
   não decidida (os colecionáveis, por exemplo). */
public record PrevisaoItemRequestDTO(
        @NotBlank String descricao,
        @NotNull Integer categoriaId,
        Integer fornecedorId,
        @NotNull @PositiveOrZero Integer quantidade,
        @NotNull @PositiveOrZero BigDecimal valorUnitario,
        @PositiveOrZero BigDecimal frete,
        @NotBlank String escala,
        @NotBlank String status,
        LocalDate dataPrevista,
        @Size(max = 500) String observacao
) {}
