package com.semac.java_api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/* `fator` é o multiplicador que as escalas aplicaram: 1 sem escala (valor
   fechado), senão a soma dos contadores das escalas marcadas. Vai explícito na resposta para a
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
        List<String> escalas,
        Integer fator,
        BigDecimal valorTotal,
        String status,
        LocalDate dataPrevista,
        String observacao,
        Integer compraId
) {}
