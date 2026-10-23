package com.semac.java_api.dto;

/* O que a página /mostra/<codigo> mostra depois da leitura do QR do stand.

   `jaVisitado` = a pessoa já tinha lido este QR antes; não é erro, a tela
   só troca o texto. `conquistaConcedida` = esta leitura fechou todos os
   trabalhos e a conquista MOSTRA_COMPLETA saiu agora. */
public record VisitaMostraRespostaDTO(
        String titulo,
        String apresentadores,
        boolean jaVisitado,
        int visitados,
        int total,
        boolean conquistaConcedida
) {}
