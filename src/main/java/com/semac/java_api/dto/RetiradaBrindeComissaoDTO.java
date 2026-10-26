package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/* Leitura do QR do brinde no /checkin (POST /api/brinde-comissao/retirada).
   `qrCode` é o texto lido, com o prefixo. Quem entrega sai do token. */
public record RetiradaBrindeComissaoDTO(
        @NotBlank @Size(max = 120) String qrCode
) {}
