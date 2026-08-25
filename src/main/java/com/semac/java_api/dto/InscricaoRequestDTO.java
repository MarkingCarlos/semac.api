package com.semac.java_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/* Cadastro público em /inscricoes. O ingresso escolhido pela pessoa é
   gravado já aqui (o organizador ainda pode trocá-lo na confirmação).
   `dias` só se aplica a ingresso de diária; `camisetas` traz uma entrada
   por camiseta pedida — as gratuitas do ingresso e as avulsas. */
public record InscricaoRequestDTO(
        @NotBlank String nome,
        @NotBlank String cpf,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String senha,
        String ra,
        @NotNull Integer tipoInscricaoId,
        @Min(1) Integer dias,
        @Valid List<CamisetaPedidoDTO> camisetas
) {}
