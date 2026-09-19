package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;

public record TermoPalpiteDTO(
        @NotBlank String palpite
) {}
