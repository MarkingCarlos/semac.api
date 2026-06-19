package com.semac.java_api.exception;

import com.semac.java_api.dto.ErroRespostaDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/* Centraliza o tratamento de erros da API, devolvendo sempre um corpo
   JSON { mensagem } que o front exibe como aviso. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* Recurso já existente (e-mail / CPF duplicado) → 409 Conflict. */
    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErroRespostaDTO> tratarRecursoDuplicado(RecursoDuplicadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroRespostaDTO(ex.getMessage()));
    }

    /* Falha de validação dos DTOs (@Valid) → 400 com a primeira mensagem. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroRespostaDTO> tratarValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(erro -> erro.getDefaultMessage())
                .orElse("Dados inválidos.");
        return ResponseEntity.badRequest().body(new ErroRespostaDTO(mensagem));
    }

    /* Erros lançados com status explícito (ex.: 404 não encontrado,
       400 papel inválido) → repassa o status com corpo { mensagem }. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroRespostaDTO> tratarStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ErroRespostaDTO(ex.getReason()));
    }

    /* Rede de segurança: violação de restrição única no banco que tenha
       escapado da checagem prévia → 409 em vez de 500. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroRespostaDTO> tratarIntegridade(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroRespostaDTO("Já existe um registro com esses dados."));
    }
}
