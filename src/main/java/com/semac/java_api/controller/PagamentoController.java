package com.semac.java_api.controller;

import com.semac.java_api.dto.CriarPagamentoCartaoRequestDTO;
import com.semac.java_api.dto.PagamentoCartaoResponseDTO;
import com.semac.java_api.service.PagamentoCartaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagamento")
public class PagamentoController {

    private final PagamentoCartaoService pagamentoCartaoService;

    public PagamentoController(PagamentoCartaoService pagamentoCartaoService) {
        this.pagamentoCartaoService = pagamentoCartaoService;
    }

    /* Público — chamado logo depois de POST /api/inscricao, mesmo momento
       em que hoje se envia o comprovante do Pix (ver SecurityConfig). */
    @PostMapping("/cartao")
    public ResponseEntity<PagamentoCartaoResponseDTO> criarPagamentoCartao(
            @Valid @RequestBody CriarPagamentoCartaoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagamentoCartaoService.criarPagamento(dto));
    }

    /* Admin-only (ver SecurityConfig) — reconsulta o status na Mercado Pago
       para os raros casos pending/in_process que resolvem depois. */
    @GetMapping("/cartao/{mpPaymentId}/status")
    public PagamentoCartaoResponseDTO reconsultarStatus(@PathVariable Long mpPaymentId) {
        return pagamentoCartaoService.reconsultarStatus(mpPaymentId);
    }
}
