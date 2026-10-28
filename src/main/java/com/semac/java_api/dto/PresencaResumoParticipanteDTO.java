package com.semac.java_api.dto;

import java.time.LocalDateTime;

/* Um check-in confirmado de um participante, como a página de resumo do
   /admin mostra. `registradoPorNome` vem null em presenças marcadas antes
   da V47 (sem registro de quem leu o QR). */
public record PresencaResumoParticipanteDTO(
        Integer eventoId,
        String eventoNome,
        String tipoEventoNome,
        LocalDateTime dataHoraInicio,
        LocalDateTime presencaEm,
        Integer xpCreditado,
        String registradoPorNome
) {}
