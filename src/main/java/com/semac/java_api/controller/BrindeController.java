package com.semac.java_api.controller;

import com.semac.java_api.dto.BrindeRequestDTO;
import com.semac.java_api.dto.BrindeResponseDTO;
import com.semac.java_api.model.Brinde;
import com.semac.java_api.model.Sorteio;
import com.semac.java_api.repository.BrindeRepository;
import com.semac.java_api.repository.GanhadoresSorteioRepository;
import com.semac.java_api.repository.SorteioRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

/* CRUD dos brindes sorteados (tabela `brinde`). Gerenciado na aba
   "Brindes" do /admin — nome e o sorteio ao qual pertence. Cada brinde é
   uma unidade; `entregue` vem de haver entrega vinculada (ver
   GanhadoresSorteioRepository.countByBrinde_Id), sem coluna própria. Excluir um brinde já entregue é barrado pela FK
   (DataIntegrityViolationException → 409 no GlobalExceptionHandler). */
@RestController
@RequestMapping("/api/brinde")
public class BrindeController {

    private final BrindeRepository brindeRepository;
    private final SorteioRepository sorteioRepository;
    private final GanhadoresSorteioRepository ganhadoresSorteioRepository;

    public BrindeController(BrindeRepository brindeRepository, SorteioRepository sorteioRepository,
                            GanhadoresSorteioRepository ganhadoresSorteioRepository) {
        this.brindeRepository = brindeRepository;
        this.sorteioRepository = sorteioRepository;
        this.ganhadoresSorteioRepository = ganhadoresSorteioRepository;
    }

    /* `sorteioId` opcional filtra os brindes de um sorteio (tela /sorteio). */
    @GetMapping
    public List<BrindeResponseDTO> listar(@RequestParam(required = false) Integer sorteioId) {
        List<Brinde> brindes = sorteioId == null
                ? brindeRepository.findAll()
                : brindeRepository.findBySorteio_Id(sorteioId);
        return brindes.stream()
                .sorted(Comparator.comparing(b -> b.getNome().toLowerCase()))
                .map(this::paraResposta)
                .toList();
    }

    @PostMapping
    public ResponseEntity<BrindeResponseDTO> criar(@Valid @RequestBody BrindeRequestDTO dto) {
        Brinde brinde = new Brinde();
        brinde.setNome(dto.nome());
        brinde.setSorteio(buscarSorteio(dto.sorteioId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(brindeRepository.save(brinde)));
    }

    @PutMapping("/{id}")
    public BrindeResponseDTO atualizar(@PathVariable Integer id, @Valid @RequestBody BrindeRequestDTO dto) {
        Brinde brinde = brindeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brinde não encontrado."));

        long entregue = ganhadoresSorteioRepository.countByBrinde_Id(id);
        // As entregas guardam o sorteio em que aconteceram; mover o brinde
        // depois disso deixaria as duas informações divergentes.
        boolean trocouSorteio = !brinde.getSorteio().getId().equals(dto.sorteioId());
        if (trocouSorteio && entregue > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse brinde já foi entregue — não pode mais trocar de sorteio.");
        }

        brinde.setNome(dto.nome());
        if (trocouSorteio) {
            brinde.setSorteio(buscarSorteio(dto.sorteioId()));
        }
        return paraResposta(brindeRepository.save(brinde));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!brindeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        if (ganhadoresSorteioRepository.countByBrinde_Id(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esse brinde já foi entregue e não pode ser excluído.");
        }
        brindeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Sorteio buscarSorteio(Integer id) {
        return sorteioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sorteio não encontrado."));
    }

    private BrindeResponseDTO paraResposta(Brinde brinde) {
        boolean entregue = ganhadoresSorteioRepository.countByBrinde_Id(brinde.getId()) > 0;
        return new BrindeResponseDTO(brinde.getId(), brinde.getNome(), entregue,
                brinde.getSorteio().getId(), brinde.getSorteio().getNome());
    }
}
