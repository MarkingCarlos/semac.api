package com.semac.java_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/* Cobrança do cartão de crédito parcelado (Mercado Pago), enviada depois
   que a inscrição já existe (/api/inscricao). Sem campo de valor: o total
   é sempre recalculado no backend a partir do ingresso e das camisetas
   avulsas da pessoa — nunca confiar em valor vindo do cliente. */
public record CriarPagamentoCartaoRequestDTO(
        @NotBlank String pessoaUuid,
        @NotBlank String token,
        @NotBlank String paymentMethodId,
        String issuerId,
        @NotNull @Min(1) Integer installments,
        @NotBlank @Email String payerEmail,
        @NotBlank String payerCpf
) {}
