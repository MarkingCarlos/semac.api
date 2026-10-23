package com.semac.java_api.dto;

import java.time.LocalDateTime;

/* Pergunta como o /admin vê: com o nome de quem enviou. O participante
   nunca recebe esta lista.

   `nomeParticipante` é o nome da conta (com login), o nome digitado (sem
   login) ou "Anônimo". `autorIdentificado` = veio de uma conta; false quer
   dizer que o nome, se houver, foi digitado e não é verificado.
   `nomeCadastradaPor` = diretor/presidente que incluiu a pergunta pelo
   /admin; null quando veio do público. */
public record PerguntaDebateResponseDTO(
        Integer id,
        String texto,
        String nomeParticipante,
        boolean autorIdentificado,
        String nomeCadastradaPor,
        LocalDateTime enviadaEm
) {}
