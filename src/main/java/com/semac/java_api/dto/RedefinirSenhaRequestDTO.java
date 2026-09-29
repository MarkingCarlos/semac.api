package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/* Etapa 3 da recuperação de senha. As regras da senha espelham o
   checklist do cadastro no site (BoxInscricao): 8+ caracteres, uma
   maiúscula e um caractere especial. */
public record RedefinirSenhaRequestDTO(
        @NotBlank String tokenTroca,
        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 8, message = "A senha precisa ter pelo menos 8 caracteres.")
        @Pattern(regexp = ".*[A-Z].*", message = "A senha precisa ter uma letra maiúscula.")
        @Pattern(regexp = ".*[^a-zA-Z0-9].*", message = "A senha precisa ter um caractere especial.")
        String novaSenha
) {}
