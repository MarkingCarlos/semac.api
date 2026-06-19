package com.semac.java_api.controller;

import com.semac.java_api.dto.CompraRequestDTO;
import com.semac.java_api.dto.CompraResponseDTO;
import com.semac.java_api.model.Compra;
import com.semac.java_api.model.Fornecedor;
import com.semac.java_api.repository.CompraRepository;
import com.semac.java_api.repository.FornecedorRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/compra")
public class CompraController {

    private final CompraRepository compraRepository;
    private final FornecedorRepository fornecedorRepository;

    public CompraController(CompraRepository compraRepository,
                           FornecedorRepository fornecedorRepository) {
        this.compraRepository = compraRepository;
        this.fornecedorRepository = fornecedorRepository;
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

    @PostMapping
    public ResponseEntity<CompraResponseDTO> criar(@Valid @RequestBody CompraRequestDTO dto) {
        Compra compra = new Compra();
        aplicar(compra, dto);
        compra.setDataCompra(LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(compraRepository.save(compra)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompraResponseDTO> atualizar(@PathVariable Integer id,
                                                       @Valid @RequestBody CompraRequestDTO dto) {
        return compraRepository.findById(id)
                .map(compra -> {
                    aplicar(compra, dto); // preserva a dataCompra original
                    return ResponseEntity.ok(paraResposta(compraRepository.save(compra)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!compraRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        compraRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void aplicar(Compra compra, CompraRequestDTO dto) {
        Fornecedor fornecedor = fornecedorRepository.findById(dto.fornecedorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Fornecedor inválido."));

        compra.setDescricao(dto.descricao());
        compra.setCategoria(dto.categoria());
        compra.setFornecedor(fornecedor);
        compra.setValorUnitario(dto.valorUnitario());
        compra.setQuantidade(dto.quantidade());
        // valor_total é calculado pelo backend
        compra.setValorTotal(dto.valorUnitario().multiply(BigDecimal.valueOf(dto.quantidade())));
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
                compra.getDataCompra()
        );
    }
}
