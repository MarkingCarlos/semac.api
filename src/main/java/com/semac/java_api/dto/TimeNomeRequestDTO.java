package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TimeNomeRequestDTO(
        @NotBlank @Size(max = 40) String nome
) {}
