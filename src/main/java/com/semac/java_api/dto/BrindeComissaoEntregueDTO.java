package com.semac.java_api.dto;

/* Resultado de uma retirada no /checkin: de quem era o QR (nome e papel
   na comissão), para a tela de sucesso. */
public record BrindeComissaoEntregueDTO(
        String nome,
        String role
) {}
