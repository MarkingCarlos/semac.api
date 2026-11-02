package com.semac.java_api.controller;

import com.semac.java_api.dto.CompraResponseDTO;
import com.semac.java_api.model.Compra;
import com.semac.java_api.repository.CompraRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compra")
public class CompraController {

    /* Só leitura: a compra nasce e morre pela previsão
       (POST /api/previsao/{id}/converter e /reverter). */
    private final CompraRepository compraRepository;

    public CompraController(CompraRepository compraRepository) {
        this.compraRepository = compraRepository;
    }

    @GetMapping
    public List<CompraResponseDTO> listar() {
        return compraRepository.findAllByOrderByDataCompraDesc().stream()
                .map(this::paraResposta)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompraResponseDTO> buscar(@PathVariable Integer id) {
        return compraRepository.findById(id)
                .map(compra -> ResponseEntity.ok(paraResposta(compra)))
                .orElse(ResponseEntity.notFound().build());
    }

    private CompraResponseDTO paraResposta(Compra compra) {
        return new CompraResponseDTO(
                compra.getId(),
                compra.getDescricao(),
                compra.getCategoria(),
                compra.getFornecedor().getId(),
                compra.getValorUnitario(),
                compra.getQuantidade(),
                compra.getValorTotal(),
                compra.getDataCompra(),
                compra.getStatus().name()
        );
    }
}
