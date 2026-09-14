package com.semac.java_api.model.enums;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/* Estágio de um gasto previsto, do orçamento ao pagamento.
   PAGO é terminal: o item ganha um `compra` correspondente e passa a
   contar como realizado, não mais como previsão. */
public enum StatusPrevisao {
    PREVISTO,
    COTADO,
    CONTRATADO,
    PAGO;

    public static StatusPrevisao deTexto(String texto) {
        if (texto == null || texto.isBlank()) return PREVISTO;
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status inválido: " + texto);
        }
    }
}
