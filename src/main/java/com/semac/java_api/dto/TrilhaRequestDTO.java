package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;

public record TrilhaRequestDTO(
        @NotBlank String nome
) {}
