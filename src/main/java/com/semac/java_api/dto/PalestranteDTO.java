package com.semac.java_api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/* Palestrante de um evento (relação N:N `evento_palestrante`).
   `id` é null na criação; preenchido na resposta. Com `id`, a gravação do
   evento atualiza aquele palestrante em vez de recriar — é o que preserva
   a foto entre uma edição e outra.

   `temFoto` só vale na resposta (a foto sobe à parte, por
   POST /api/palestrante/{id}/foto); na requisição é ignorado.

   `fotoPosX`/`fotoPosY`/`fotoZoom` são o enquadramento da foto na moldura
   do telão do debate; null na requisição mantém o que estava. */
public record PalestranteDTO(
        Integer id,
        @NotBlank String nome,
        @Size(max = 2000) String descricao,
        @Size(max = 150) String cargo,
        Boolean temFoto,
        @DecimalMin("0") @DecimalMax("100") BigDecimal fotoPosX,
        @DecimalMin("0") @DecimalMax("100") BigDecimal fotoPosY,
        @DecimalMin("1") @DecimalMax("3") BigDecimal fotoZoom
) {}
