package com.semac.java_api.dto;

import java.util.List;

public record ConjuntoResponseDTO(
        Integer id,
        String nome,
        List<VariacaoResponseDTO> variacoes
) {}
