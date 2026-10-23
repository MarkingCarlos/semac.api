package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/* Tentativa do modo teste (/desafio-cifra/teste): sem jogo no servidor, a
   tela diz de qual desafio e etapa é a resposta. */
public record DesafioCifraTesteTentativaDTO(
        @NotNull Integer desafioId,
        @NotNull Integer ordem,
        @NotBlank @Size(max = 100) String resposta
) {}
