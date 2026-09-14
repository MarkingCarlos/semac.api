package com.semac.java_api.model.enums;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/* As duas contas que a SEMAC movimenta: a da comissão (pessoa física) e
   a da FUNDUNESP. Toda entrada tem conta destinatária e toda saída tem
   conta de origem — é o que permite fechar o balanço por conta. */
public enum ContaFinanceira {
    COMISSAO,
    FUNDUNESP;

    /* Texto vazio ou ausente vira null: a conta é legitimamente
       desconhecida enquanto a comissão não a define. Só texto preenchido
       e inválido é rejeitado. */
    public static ContaFinanceira deTexto(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Conta inválida: " + texto + ". Use COMISSAO ou FUNDUNESP.");
        }
    }
}
