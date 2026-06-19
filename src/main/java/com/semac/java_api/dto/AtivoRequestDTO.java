package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;

/* Ativa/desativa uma pessoa (ex.: suspender um membro da comissão).
   ativo=false não remove o registro — preserva histórico. */
public record AtivoRequestDTO(@NotNull Boolean ativo) {}
