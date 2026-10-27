package com.semac.java_api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/* Uma conquista do catálogo, como o /admin a vê.

   `totalConquistado` é quantos participantes já a possuem: é o que explica
   na interface por que o botão de desativar está travado (desativar com
   gente vinculada devolve 409 — ver ConquistaService.alterarAtiva).

   `imagemVersao` é o nome do arquivo em disco, e vem null quando não há
   imagem — é assim que o front sabe se existe uma. Serve também de
   cache-buster: a URL da imagem (GET /api/conquista/{id}/imagem) é fixa e
   tem max-age de um dia, então sem um `?v=` que mude a cada upload o
   /admin continuaria mostrando a imagem antiga depois de trocá-la.

   `concessaoAte` é o último dia em que a comissão concede esta conquista
   manual (null = sem prazo; ver CatalogoConquistas.PRAZOS_CONCESSAO_MANUAL).
   Diretoria de site e presidência concedem mesmo depois dele.

   `imagemPosX`/`imagemPosY`/`imagemZoom` são o enquadramento da imagem na
   moldura dos cards (ver Conquista e V76). */
public record ConquistaResponseDTO(
        Integer id,
        String codigo,
        String nome,
        String descricao,
        Integer pontosBase,
        Integer raridade,
        Integer ordem,
        Boolean ativa,
        String tipoValidacao,
        String imagemVersao,
        long totalConquistado,
        LocalDate concessaoAte,
        BigDecimal imagemPosX,
        BigDecimal imagemPosY,
        BigDecimal imagemZoom
) {}
