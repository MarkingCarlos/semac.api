package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/* `genero`: 'M' (crachá PROFESSOR) ou 'F' (crachá PROFESSORA). */
public record ProfessorConvidadoRequestDTO(
        @NotBlank String nome,
        @NotNull @Pattern(regexp = "[MF]", message = "Gênero inválido.") String genero
) {}
