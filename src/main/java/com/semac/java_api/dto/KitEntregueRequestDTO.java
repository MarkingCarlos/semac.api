package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;

/* Marca (entregue=true) ou desfaz (entregue=false) a entrega do kit.
   Quem entregou sai do token, nunca do corpo. */
public record KitEntregueRequestDTO(@NotNull Boolean entregue) {}
