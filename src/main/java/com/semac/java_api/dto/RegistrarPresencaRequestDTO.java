package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;

/* Corpo do POST /api/evento/{id}/presenca — uuid lido do QR code do
   crachá do participante (ver QrCrachaParticipantes.jsx no frontend). */
public record RegistrarPresencaRequestDTO(
        @NotBlank(message = "Informe o QR code do participante.")
        String uuid
) {}
