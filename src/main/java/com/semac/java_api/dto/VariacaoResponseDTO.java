package com.semac.java_api.dto;

import java.util.List;

public record VariacaoResponseDTO(
        Integer id,
        Integer conjuntoId,
        String nome,
        List<VariacaoItemResponseDTO> itens
) {}
