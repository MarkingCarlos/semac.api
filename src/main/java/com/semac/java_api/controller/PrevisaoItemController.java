package com.semac.java_api.controller;

import com.semac.java_api.dto.PrevisaoItemRequestDTO;
import com.semac.java_api.dto.PrevisaoItemResponseDTO;
import com.semac.java_api.dto.PrevisaoResumoDTO;
import com.semac.java_api.model.Compra;
import com.semac.java_api.model.Fornecedor;
import com.semac.java_api.model.PrevisaoCategoria;
import com.semac.java_api.model.PrevisaoItem;
import com.semac.java_api.model.enums.EscalaPrevisao;
import com.semac.java_api.model.enums.StatusCompra;
import com.semac.java_api.model.enums.StatusPrevisao;
import com.semac.java_api.repository.CompraRepository;
import com.semac.java_api.repository.FornecedorRepository;
import com.semac.java_api.repository.PrevisaoCategoriaRepository;
import com.semac.java_api.repository.PrevisaoItemRepository;
import com.semac.java_api.service.PrevisaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/previsao")
public class PrevisaoItemController {

    private final PrevisaoItemRepository itemRepository;
    private final PrevisaoCategoriaRepository categoriaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final CompraRepository compraRepository;
    private final PrevisaoService previsaoService;

    public PrevisaoItemController(PrevisaoItemRepository itemRepository,
                                  PrevisaoCategoriaRepository categoriaRepository,
                                  FornecedorRepository fornecedorRepository,
                                  CompraRepository compraRepository,
                                  PrevisaoService previsaoService) {
        this.itemRepository = itemRepository;
        this.categoriaRepository = categoriaRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.compraRepository = compraRepository;
        this.previsaoService = previsaoService;
    }

    @GetMapping
    public List<PrevisaoItemResponseDTO> listar() {
        return previsaoService.listarItens();
    }

    /* Consolidado que alimenta os gráficos do dashboard. */
    @GetMapping("/resumo")
    public PrevisaoResumoDTO resumo() {
        return previsaoService.resumo();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrevisaoItemResponseDTO> buscar(@PathVariable Integer id) {
        return itemRepository.findById(id)
                .map(item -> ResponseEntity.ok(
                        previsaoService.paraResposta(item, previsaoService.fatoresVigentes())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PrevisaoItemResponseDTO> criar(@Valid @RequestBody PrevisaoItemRequestDTO dto) {
        PrevisaoItem item = aplicar(new PrevisaoItem(), dto);
        PrevisaoItem salvo = itemRepository.save(item);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(previsaoService.paraResposta(salvo, previsaoService.fatoresVigentes()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrevisaoItemResponseDTO> atualizar(@PathVariable Integer id,
                                                             @Valid @RequestBody PrevisaoItemRequestDTO dto) {
        return itemRepository.findById(id)
                .map(item -> {
                    PrevisaoItem salvo = itemRepository.save(aplicar(item, dto));
                    return ResponseEntity.ok(
                            previsaoService.paraResposta(salvo, previsaoService.fatoresVigentes()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!itemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        itemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /* Previsão vira gasto real: cria a `compra` correspondente e marca o
       item como PAGO, para ele deixar de contar como valor ainda por
       gastar. É o caminho de mão única entre as duas tabelas.

       Ressalva: `compra` não tem campo de frete, então o frete da
       previsão entra embutido no valorTotal da compra. Uma edição
       posterior da compra pelo /api/compra recalcula valorTotal como
       valorUnitario × quantidade e descarta esse embutido. */
    @PostMapping("/{id}/converter")
    public ResponseEntity<PrevisaoItemResponseDTO> converterEmCompra(@PathVariable Integer id) {
        PrevisaoItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Previsão não encontrada."));

        if (item.getCompra() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta previsão já foi convertida na compra #" + item.getCompra().getId() + ".");
        }
        Fornecedor fornecedor = item.getFornecedor();
        if (fornecedor == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Defina o fornecedor da previsão antes de convertê-la em compra.");
        }

        int quantidadeFinal = item.getQuantidade()
                * previsaoService.fator(item, previsaoService.fatoresVigentes());

        Compra compra = new Compra();
        compra.setDescricao(item.getDescricao());
        compra.setCategoria(item.getCategoria().getNome());
        compra.setFornecedor(fornecedor);
        compra.setValorUnitario(item.getValorUnitario());
        compra.setQuantidade(quantidadeFinal);
        compra.setValorTotal(previsaoService.valorTotal(item, previsaoService.fatoresVigentes()));
        compra.setDataCompra(LocalDateTime.now());
        compra.setStatus(StatusCompra.PAGO);

        item.setCompra(compraRepository.save(compra));
        item.setStatus(StatusPrevisao.PAGO);
        PrevisaoItem salvo = itemRepository.save(item);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(previsaoService.paraResposta(salvo, previsaoService.fatoresVigentes()));
    }

    private PrevisaoItem aplicar(PrevisaoItem item, PrevisaoItemRequestDTO dto) {
        PrevisaoCategoria categoria = categoriaRepository.findById(dto.categoriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria inválida."));

        Fornecedor fornecedor = dto.fornecedorId() == null ? null
                : fornecedorRepository.findById(dto.fornecedorId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fornecedor inválido."));

        item.setDescricao(dto.descricao());
        item.setCategoria(categoria);
        item.setFornecedor(fornecedor);
        item.setQuantidade(dto.quantidade());
        item.setValorUnitario(dto.valorUnitario());
        item.setFrete(dto.frete() == null ? BigDecimal.ZERO : dto.frete());
        item.setEscala(EscalaPrevisao.deTexto(dto.escala()));
        item.setStatus(StatusPrevisao.deTexto(dto.status()));
        item.setDataPrevista(dto.dataPrevista());
        item.setObservacao(dto.observacao());
        return item;
    }
}
