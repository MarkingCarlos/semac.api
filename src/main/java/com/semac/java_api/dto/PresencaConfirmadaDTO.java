package com.semac.java_api.dto;

/* Resposta de sucesso do check-in (leitura de QR ou confirmação manual),
   usada pela ferramenta /checkin para exibir quem acabou de entrar.
   xpGanho e atrasoMinutos refletem a regra de atraso aplicada nesse
   check-in (ver InscricaoEventoService.marcarPresente). */
public record PresencaConfirmadaDTO(
        String nome,
        String infoAdicional,
        Integer xpGanho,
        Long atrasoMinutos
) {}
