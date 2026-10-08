package com.semac.java_api.dto;

/* Resposta do modo "Testar leitura" do /checkin: só o suficiente para
   conferir que o QR do crachá é lido e aponta para a pessoa certa.
   `situacao` é um rótulo pronto para exibir ("Participante confirmado",
   "Aguardando confirmação", a função na comissão…). */
public record LeituraTesteQrDTO(
        String nome,
        String situacao
) {}
