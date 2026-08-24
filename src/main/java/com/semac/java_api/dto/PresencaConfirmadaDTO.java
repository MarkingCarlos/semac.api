package com.semac.java_api.dto;

/* Resposta de sucesso do check-in (leitura de QR ou confirmação manual),
   usada pela ferramenta /checkin para exibir quem acabou de entrar. */
public record PresencaConfirmadaDTO(
        String nome,
        String infoAdicional
) {}
