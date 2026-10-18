package com.semac.java_api.dto;

/* Resposta de um palpite de teste: o padrão de cores e se acertou. Não há
   `palavra` nem contagem de tentativas — o teste não guarda estado no
   servidor, então revelar a palavra no fim seria revelá-la a qualquer
   pedido. */
public record TermoPalpiteTesteRespostaDTO(
        String resultado,
        boolean venceu
) {}
