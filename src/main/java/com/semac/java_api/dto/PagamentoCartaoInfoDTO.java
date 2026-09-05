package com.semac.java_api.dto;

import java.math.BigDecimal;

/* Evidência do pagamento por cartão exibida ao admin em /admin, no lugar
   do comprovante de Pix — não confirma a inscrição sozinha, quem confirma
   continua sendo o admin em PessoaService.atribuirRole. */
public record PagamentoCartaoInfoDTO(
        Long mpPaymentId,
        String status,
        String statusDetail,
        Integer parcelas,
        BigDecimal valorCobrado
) {}
