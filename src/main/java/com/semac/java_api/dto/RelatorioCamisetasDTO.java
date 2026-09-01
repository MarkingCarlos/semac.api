package com.semac.java_api.dto;

import java.util.List;

/* Relatório de camisetas do /admin: quanto comprar no total, dividido
   entre dadas (inclusas no ingresso) e avulsas (compra extra), mais o
   detalhamento por modelo/tamanho para orientar a compra real. */
public record RelatorioCamisetasDTO(
        int totalGeral,
        int totalDadas,
        int totalAvulsas,
        List<ItemEstoqueCamisetaDTO> porModeloTamanho
) {}
