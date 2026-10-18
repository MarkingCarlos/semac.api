package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/* Cadastro rápido feito no painel de edição do evento (/admin ->
   Conteúdo -> Eventos): o que vai ser sorteado. Vira um sorteio com esse
   nome e um brinde (uma unidade) dentro dele. */
public record SorteioComBrindeRequestDTO(
        @NotNull Integer eventoId,
        @NotBlank String brinde
) {}
