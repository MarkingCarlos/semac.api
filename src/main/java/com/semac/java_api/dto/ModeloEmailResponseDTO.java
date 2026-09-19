package com.semac.java_api.dto;

import java.util.List;

public record ModeloEmailResponseDTO(
        String chave,
        String nomeExibicao,
        String descricao,
        String assunto,
        String corpoMarkdown,
        Boolean ativo,
        String atualizadoEm,
        String atualizadoPor,
        List<VariavelEmailDTO> variaveis
) {}
