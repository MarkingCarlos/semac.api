package com.semac.java_api.model.enums;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/* Como o valor de um item previsto se multiplica.

   FIXA é o caso comum: o total é o próprio valor lançado. As demais
   multiplicam pelo contador correspondente em `orcamento`, para que uma
   previsão por cabeça (o kit do participante, por exemplo) acompanhe
   sozinha a mudança na estimativa de público. */
public enum EscalaPrevisao {
    FIXA,
    POR_INSCRITO,
    POR_COMISSAO,
    POR_PALESTRANTE;

    public static EscalaPrevisao deTexto(String texto) {
        if (texto == null || texto.isBlank()) return FIXA;
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escala inválida: " + texto);
        }
    }
}
