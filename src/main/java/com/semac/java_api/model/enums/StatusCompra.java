package com.semac.java_api.model.enums;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/* Uma compra registrada é, por padrão, dinheiro que já saiu. PENDENTE
   cobre a compra fechada com o fornecedor e ainda não paga. */
public enum StatusCompra {
    PENDENTE,
    PAGO;

    public static StatusCompra deTexto(String texto) {
        if (texto == null || texto.isBlank()) return PAGO;
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status inválido: " + texto);
        }
    }
}
