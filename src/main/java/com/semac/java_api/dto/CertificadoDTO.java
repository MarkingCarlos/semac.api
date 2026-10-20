package com.semac.java_api.dto;

import java.time.LocalDateTime;

/* Um certificado de palestrante (subaba Certificados da aba Pessoas do
   /admin): uma linha por par palestrante × evento da `evento_palestrante`.
   Evento com mais de um palestrante gera uma linha para cada um.

   `codigoTipoEvento` (PALESTRA, MINICURSO, MESA_REDONDA...) escolhe a frase
   do certificado no navegador ("ministrou a palestra intitulada", "ministrou
   o minicurso intitulado"...). A montagem do PDF acontece toda no front. */
public record CertificadoDTO(
        Integer palestranteId,
        String nomePalestrante,
        Integer eventoId,
        String nomeEvento,
        String codigoTipoEvento,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim
) {}
