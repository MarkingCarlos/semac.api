package com.semac.java_api.dto;

/* Um trabalho da Mostra Técnica como a diretoria o vê no /admin.
   `codigoQr` é o que a tela usa para montar o link /mostra/<codigoQr> e
   gerar o QR code do stand. */
public record TrabalhoMostraAdminDTO(
        Integer id,
        String titulo,
        String apresentadores,
        String codigoQr,
        long visitas
) {}
