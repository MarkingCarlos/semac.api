package com.semac.java_api.dto;

import java.time.LocalDateTime;

/* Resposta de GET /api/brinde-comissao/meu (cartão do Início do /admin).

   `disponivelHoje` false: hoje não é o dia do brinde — o cartão não
   aparece e nenhum outro campo vem preenchido. `qrCode` é o texto do QR
   (prefixo + código); vem null depois da retirada, quando `retiradoEm`
   e `entreguePorNome` dizem quando e com quem foi. */
public record MeuBrindeComissaoDTO(
        boolean disponivelHoje,
        String qrCode,
        LocalDateTime retiradoEm,
        String entreguePorNome
) {
    public static MeuBrindeComissaoDTO indisponivel() {
        return new MeuBrindeComissaoDTO(false, null, null, null);
    }
}
