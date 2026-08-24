package com.semac.java_api.controller;

import com.semac.java_api.dto.BrindeRequestDTO;
import com.semac.java_api.dto.BrindeResponseDTO;
import com.semac.java_api.model.Brinde;
import com.semac.java_api.repository.BrindeRepository;
import com.semac.java_api.repository.SorteioRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

/* CRUD dos brindes sorteados (tabela `brinde`). Gerenciado na aba
   "Brindes" do /admin — apenas nome e quantidade em estoque.
   `quantidadeEntregue` é calculada contando os sorteios vinculados
   (ver SorteioRepository.countByBrinde_Id), sem coluna acumuladora.
   Excluir um brinde com sorteios vinculados é barrado pela FK
   (DataIntegrityViolationException → 409 no GlobalExceptionHandler). */
@RestController
@RequestMapping("/api/brinde")
public class BrindeController {

    private final BrindeRepository brindeRepository;
    private final SorteioRepository sorteioRepository;

    public BrindeController(BrindeRepository brindeRepository, SorteioRepository sorteioRepository) {
        this.brindeRepository = brindeRepository;
        this.sorteioRepository = sorteioRepository;
    }

    @GetMapping
    public List<BrindeResponseDTO> listar() {
        return brindeRepository.findAll().stream()
                .sorted(Comparator.comparing(b -> b.getNome().toLowerCase()))
                .map(this::paraResposta)
                .toList();
    }

    @PostMapping
    public ResponseEntity<BrindeResponseDTO> criar(@Valid @RequestBody BrindeRequestDTO dto) {
        Brinde brinde = new Brinde();
        brinde.setNome(dto.nome());
        brinde.setQuantidade(dto.quantidade());
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(brindeRepository.save(brinde)));
    }

    @PutMapping("/{id}")
    public BrindeResponseDTO atualizar(@PathVariable Integer id, @Valid @RequestBody BrindeRequestDTO dto) {
        Brinde brinde = brindeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brinde não encontrado."));

        long entregue = sorteioRepository.countByBrinde_Id(id);
        if (dto.quantidade() < entregue) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já foram entregues " + entregue + " unidades — a quantidade não pode ser menor que isso.");
        }

        brinde.setNome(dto.nome());
        brinde.setQuantidade(dto.quantidade());
        return paraResposta(brindeRepository.save(brinde));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!brindeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        brindeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private BrindeResponseDTO paraResposta(Brinde brinde) {
        long entregue = sorteioRepository.countByBrinde_Id(brinde.getId());
        return new BrindeResponseDTO(brinde.getId(), brinde.getNome(), brinde.getQuantidade(), (int) entregue);
    }
}
