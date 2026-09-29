package com.semac.java_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/* Etapa 2 da recuperação de senha: o código de 5 dígitos recebido. */
public record VerificarCodigoSenhaRequestDTO(
        @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email,
        @NotBlank(message = "Informe o código.")
        @Pattern(regexp = "\\d{5}", message = "O código tem 5 dígitos.") String codigo
) {}
