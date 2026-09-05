package com.semac.java_api.service;

import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.semac.java_api.dto.CriarPagamentoCartaoRequestDTO;
import com.semac.java_api.dto.PagamentoCartaoResponseDTO;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.enums.FormaPagamento;
import com.semac.java_api.repository.PessoaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

/* Cobrança do cartão de crédito parcelado na inscrição, via Mercado Pago
   (Card Payment Brick no frontend gera o token; aqui só criamos o
   pagamento). Não confirma a inscrição sozinho — quem confirma continua
   sendo o admin em PessoaService.atribuirRole, mesmo com o cartão
   aprovado (ver decisão registrada no plano desta feature). */
@Service
public class PagamentoCartaoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoCartaoService.class);
    private static final String STATUS_APROVADO = "approved";

    private final PessoaRepository pessoaRepository;
    private final PessoaService pessoaService;

    public PagamentoCartaoService(PessoaRepository pessoaRepository, PessoaService pessoaService) {
        this.pessoaRepository = pessoaRepository;
        this.pessoaService = pessoaService;
    }

    @Transactional
    public PagamentoCartaoResponseDTO criarPagamento(CriarPagamentoCartaoRequestDTO dto) {
        Pessoa pessoa = pessoaRepository.findByUuid(dto.pessoaUuid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscrição não encontrada."));

        if (STATUS_APROVADO.equals(pessoa.getMpStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta inscrição já tem um pagamento aprovado.");
        }

        BigDecimal total = pessoaService.calcularValorTotalInscricao(pessoa);

        PaymentCreateRequest request = PaymentCreateRequest.builder()
                .transactionAmount(total)
                .token(dto.token())
                .installments(dto.installments())
                .paymentMethodId(dto.paymentMethodId())
                .issuerId(dto.issuerId())
                .description("Inscrição SEMAC XXXVI — " + pessoa.getNome())
                .externalReference(pessoa.getUuid())
                .payer(PaymentPayerRequest.builder()
                        .email(dto.payerEmail())
                        .identification(IdentificationRequest.builder()
                                .type("CPF")
                                .number(dto.payerCpf())
                                .build())
                        .build())
                .build();

        Payment resposta;
        try {
            resposta = new PaymentClient().create(request);
        } catch (MPApiException e) {
            log.error("Mercado Pago recusou a criação do pagamento (pessoa uuid={}): status={} corpo={}",
                    pessoa.getUuid(), e.getApiResponse().getStatusCode(), e.getApiResponse().getContent());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível processar o pagamento. Tente novamente ou use o Pix.");
        } catch (MPException e) {
            log.error("Falha ao chamar a Mercado Pago (pessoa uuid={})", pessoa.getUuid(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível processar o pagamento. Tente novamente ou use o Pix.");
        }

        // Cartão recusado não é exceção: a Mercado Pago devolve o pagamento
        // normalmente com status "rejected" — gravamos do mesmo jeito e
        // deixamos o frontend decidir o que mostrar (tentar de novo, Pix).
        pessoa.setFormaPagamento(FormaPagamento.CARTAO);
        pessoa.setMpPaymentId(resposta.getId());
        pessoa.setMpStatus(resposta.getStatus());
        pessoa.setMpStatusDetail(resposta.getStatusDetail());
        pessoa.setParcelas(dto.installments());
        pessoa.setValorCobrado(total);
        pessoaRepository.save(pessoa);

        return new PagamentoCartaoResponseDTO(
                resposta.getId(), resposta.getStatus(), resposta.getStatusDetail(), dto.installments(), total);
    }

    /* Reconsulta o status na Mercado Pago para os raros casos que ficam
       pending/in_process na resposta síncrona e resolvem depois — usado
       pelo admin em /admin antes de confirmar, não expõe endpoint público. */
    @Transactional
    public PagamentoCartaoResponseDTO reconsultarStatus(Long mpPaymentId) {
        Pessoa pessoa = pessoaRepository.findByMpPaymentId(mpPaymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pagamento não encontrado."));

        Payment resposta;
        try {
            resposta = new PaymentClient().get(mpPaymentId);
        } catch (MPApiException e) {
            log.error("Mercado Pago recusou a consulta do pagamento {}: status={} corpo={}",
                    mpPaymentId, e.getApiResponse().getStatusCode(), e.getApiResponse().getContent());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível consultar o pagamento.");
        } catch (MPException e) {
            log.error("Falha ao chamar a Mercado Pago para consultar o pagamento {}", mpPaymentId, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível consultar o pagamento.");
        }

        pessoa.setMpStatus(resposta.getStatus());
        pessoa.setMpStatusDetail(resposta.getStatusDetail());
        pessoaRepository.save(pessoa);

        return new PagamentoCartaoResponseDTO(
                resposta.getId(), resposta.getStatus(), resposta.getStatusDetail(),
                pessoa.getParcelas(), pessoa.getValorCobrado());
    }
}
