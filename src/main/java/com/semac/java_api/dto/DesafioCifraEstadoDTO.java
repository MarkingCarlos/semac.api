package com.semac.java_api.dto;

import java.time.LocalDate;
import java.util.List;

/* Estado da Cifra do Dia de hoje, já retomando a partida em andamento.

   `etapas` traz as concluídas e a atual; as seguintes ficam de fora (a
   tela só sabe que existem por `totalEtapas`). A resposta da etapa aberta
   não vem em situação nenhuma — nem ao perder as 5 tentativas.

   `disponivel` false = hoje não tem desafio; o resto vem vazio. */
public record DesafioCifraEstadoDTO(
        boolean disponivel,
        LocalDate data,
        String palestra,
        String senhaCifrada,
        int totalEtapas,
        List<DesafioCifraEtapaDTO> etapas,
        int totalTentativas,
        int tentativasRestantes,
        boolean encerrado,
        boolean venceu,
        Integer xpGanho,
        int xpValor
) {}
