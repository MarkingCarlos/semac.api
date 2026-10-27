package com.semac.java_api.dto;

import java.time.LocalDateTime;

/* Uma linha do resumo da pesquisa de satisfação no /admin. `presentes` é
   quem teve check-in (quem pode avaliar), para a taxa de resposta;
   `media` vem null enquanto ninguém avaliou. */
public record ResumoAvaliacaoEventoDTO(
        Integer eventoId,
        String nome,
        String tipo,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        long presentes,
        long avaliacoes,
        Double media
) {}
