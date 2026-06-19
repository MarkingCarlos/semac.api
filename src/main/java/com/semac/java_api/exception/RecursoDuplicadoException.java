package com.semac.java_api.exception;

/* Lançada quando se tenta cadastrar um valor que já existe em coluna
   única (ex.: e-mail ou CPF já cadastrado). Mapeada para HTTP 409
   pelo GlobalExceptionHandler. */
public class RecursoDuplicadoException extends RuntimeException {
    public RecursoDuplicadoException(String mensagem) {
        super(mensagem);
    }
}
