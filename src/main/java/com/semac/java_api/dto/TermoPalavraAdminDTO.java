package com.semac.java_api.dto;

import java.time.LocalDate;

/* Um dos quatro dias, como a diretoria o vê em /admin -> Termo.

   `definida` no lugar da palavra, de propósito: nem quem cadastrou a lê de
   volta pela API. Mesmo critério de `codigoDefinido` no tipo de ingresso.
   Esquecer qual era significa cadastrar de novo — o custo é baixo e fecha
   o caminho de descobrir a palavra por uma conta de diretoria. */
public record TermoPalavraAdminDTO(
        Integer dia,
        LocalDate data,
        boolean definida
) {}
