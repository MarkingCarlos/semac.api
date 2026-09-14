package com.semac.java_api.controller;

import com.semac.java_api.dto.PrevisaoCategoriaRequestDTO;
import com.semac.java_api.dto.PrevisaoCategoriaResponseDTO;
import com.semac.java_api.model.PrevisaoCategoria;
import com.semac.java_api.repository.PrevisaoCategoriaRepository;
import com.semac.java_api.repository.PrevisaoItemRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/* Categorias de gasto previsto. Criar uma categoria nova não exige
   migration — é o que torna o modelo genérico preferível a uma tabela
   por tipo de gasto.

   Os totais vêm zerados aqui: quem os calcula é o /api/previsao/resumo,
   que tem o orçamento em mãos para aplicar as escalas. */
@RestController
@RequestMapping("/api/previsao-categoria")
public class PrevisaoCategoriaController {

    private final PrevisaoCategoriaRepository categoriaRepository;
    private final PrevisaoItemRepository itemRepository;

    public PrevisaoCategoriaController(PrevisaoCategoriaRepository categoriaRepository,
                                       PrevisaoItemRepository itemRepository) {
        this.categoriaRepository = categoriaRepository;
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public List<PrevisaoCategoriaResponseDTO> listar() {
        return categoriaRepository.findAllByOrderByOrdemAscNomeAsc().stream()
                .map(this::paraResposta)
                .toList();
    }

    @PostMapping
    public ResponseEntity<PrevisaoCategoriaResponseDTO> criar(@Valid @RequestBody PrevisaoCategoriaRequestDTO dto) {
        categoriaRepository.findByNomeIgnoreCase(dto.nome()).ifPresent(existente -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma categoria chamada " + existente.getNome() + ".");
        });
        PrevisaoCategoria categoria = aplicar(new PrevisaoCategoria(), dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paraResposta(categoriaRepository.save(categoria)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrevisaoCategoriaResponseDTO> atualizar(@PathVariable Integer id,
                                                                  @Valid @RequestBody PrevisaoCategoriaRequestDTO dto) {
        return categoriaRepository.findById(id)
                .map(categoria -> {
                    categoriaRepository.findByNomeIgnoreCase(dto.nome())
                            .filter(outra -> !outra.getId().equals(id))
                            .ifPresent(outra -> {
                                throw new ResponseStatusException(HttpStatus.CONFLICT,
                                        "Já existe uma categoria chamada " + outra.getNome() + ".");
                            });
                    return ResponseEntity.ok(paraResposta(categoriaRepository.save(aplicar(categoria, dto))));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /* Categoria em uso não é excluída: apagá-la levaria junto o histórico
       de previsão que aponta para ela. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!categoriaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (itemRepository.existsByCategoria_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta categoria tem itens previstos e não pode ser excluída. Mova os itens antes.");
        }
        categoriaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private PrevisaoCategoria aplicar(PrevisaoCategoria categoria, PrevisaoCategoriaRequestDTO dto) {
        categoria.setNome(dto.nome());
        categoria.setCor(dto.cor());
        categoria.setTeto(dto.teto());
        categoria.setOrdem(dto.ordem() == null ? 0 : dto.ordem());
        return categoria;
    }

    private PrevisaoCategoriaResponseDTO paraResposta(PrevisaoCategoria categoria) {
        return new PrevisaoCategoriaResponseDTO(
                categoria.getId(),
                categoria.getNome(),
                categoria.getCor(),
                categoria.getTeto(),
                categoria.getOrdem(),
                BigDecimal.ZERO,
                BigDecimal.ZERO);
    }
}
