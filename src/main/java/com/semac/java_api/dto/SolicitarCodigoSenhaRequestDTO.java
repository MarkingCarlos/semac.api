package com.semac.java_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/* Etapa 1 da recuperação de senha: para qual e-mail mandar o código. */
public record SolicitarCodigoSenhaRequestDTO(
        @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email
) {}
