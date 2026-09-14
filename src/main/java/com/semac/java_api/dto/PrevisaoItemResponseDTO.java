package com.semac.java_api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/* `fator` é o multiplicador que a escala aplicou (1 para FIXA, o nº de
   inscritos para POR_INSCRITO...). Vai explícito na resposta para a
   interface poder mostrar "R$ 98,00 × 140" sem reimplementar a regra. */
public record PrevisaoItemResponseDTO(
        Integer id,
        String descricao,
        Integer categoriaId,
        String categoriaNome,
        String categoriaCor,
        Integer fornecedorId,
        String fornecedorNome,
        Integer quantidade,
        BigDecimal valorUnitario,
        BigDecimal frete,
        String escala,
        Integer fator,
        BigDecimal valorTotal,
        String status,
        LocalDate dataPrevista,
        String observacao,
        Integer compraId
) {}
