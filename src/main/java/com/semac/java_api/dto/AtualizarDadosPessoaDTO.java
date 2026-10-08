package com.semac.java_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/* Corpo do PATCH /api/pessoa/{id}/dados — correção de cadastro feita pela
   diretoria no /admin (Diretor de Site e Presidência), tanto em
   participantes quanto em membros da comissão.

   CPF fica de fora de propósito: é o identificador da pessoa e não deve
   mudar depois do cadastro. O e-mail é o login — trocar aqui troca com
   que e-mail a pessoa entra. RA vazio vira null. Mesmas validações do
   cadastro manual (CadastroManualRequestDTO). */
public record AtualizarDadosPessoaDTO(
        @NotBlank String nome,
        @NotBlank @Email String email,
        String ra,
        @NotBlank @Pattern(regexp = "\\d{10,11}", message = "Telefone inválido.") String telefone
) {}
