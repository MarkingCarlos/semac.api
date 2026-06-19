package com.semac.java_api.dto;

public record PessoaResponseDTO(
        Integer id,
        String uuid,
        String nome,
        String email
) {}
