package com.semac.java_api.dto;

import java.util.List;

public record CotacaoResponseDTO(
        Integer id,
        String descricao,
        String categoria,
        Integer quantidade,
        List<CotacaoFornecedorResponseDTO> fornecedores
) {}
