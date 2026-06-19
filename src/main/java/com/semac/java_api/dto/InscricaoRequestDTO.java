package com.semac.java_api.dto;

import com.semac.java_api.model.enums.Modelo;
import com.semac.java_api.model.enums.Tamanho;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InscricaoRequestDTO(
        @NotBlank String nome,
        @NotBlank String cpf,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String senha,
        String ra,
        @NotNull Modelo modelo,
        @NotNull Tamanho tamanho
) {}
