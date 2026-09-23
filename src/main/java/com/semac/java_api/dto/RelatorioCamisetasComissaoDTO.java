package com.semac.java_api.dto;

import java.util.List;

/* Relatório de camisetas da comissão do /admin: total de camisetas
   exclusivas da comissão (inclusas no kit, sem avulsas) e o detalhamento
   por modelo/tamanho para o pedido ao fornecedor. */
public record RelatorioCamisetasComissaoDTO(
        int totalComissao,
        List<ItemEstoqueCamisetaDTO> porModeloTamanho
) {}
