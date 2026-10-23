package com.semac.java_api.dto;

import java.util.List;

/* Uma etapa da Cifra do Dia como o participante a vê.

   `grupos` é o tamanho de cada bloco de casas do campo (MAKE THEM BELIEVE
   = [4, 4, 7]) — é tudo o que a tela sabe da resposta enquanto a etapa
   está aberta. `resposta` só vem na etapa já concluída, que a própria
   pessoa acabou de acertar.

   `sobrescritos` diz se a resposta usa números pequenos (¹ ²), para a tela
   mostrar os botões que os digitam — o teclado do celular não tem. Não
   entrega nada que a senha cifrada já não mostre. */
public record DesafioCifraEtapaDTO(
        int ordem,
        List<Integer> grupos,
        String dica,
        boolean concluida,
        String resposta,
        boolean sobrescritos
) {}
