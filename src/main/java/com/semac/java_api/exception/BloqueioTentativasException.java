package com.semac.java_api.exception;

/* Tentativas demais — a pessoa precisa esperar antes de tentar de novo.
   Vira 429 com os segundos restantes no corpo (GlobalExceptionHandler),
   para o front mostrar a contagem regressiva. */
public class BloqueioTentativasException extends RuntimeException {

    private final long segundosRestantes;

    public BloqueioTentativasException(String mensagem, long segundosRestantes) {
        super(mensagem);
        this.segundosRestantes = segundosRestantes;
    }

    public long getSegundosRestantes() {
        return segundosRestantes;
    }
}
