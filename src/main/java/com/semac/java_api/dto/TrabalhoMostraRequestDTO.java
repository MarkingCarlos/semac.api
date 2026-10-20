package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/* Cadastro ou edição de um trabalho da Mostra Técnica no /admin. O código
   do QR não entra: é gerado pelo servidor e não muda na edição, senão o
   QR já impresso no stand pararia de funcionar. */
public record TrabalhoMostraRequestDTO(
        @NotNull Integer ano,
        @NotBlank @Size(max = 200) String titulo,
        @NotBlank @Size(max = 300) String apresentadores,
        @Size(max = 50) String stand
) {}
