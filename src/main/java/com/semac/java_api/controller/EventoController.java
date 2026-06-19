package com.semac.java_api.controller;

import com.semac.java_api.dto.EventoRequestDTO;
import com.semac.java_api.dto.EventoResponseDTO;
import com.semac.java_api.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evento")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping
    public List<EventoResponseDTO> listar() {
        return eventoService.listar();
    }

    @GetMapping("/{id}")
    public EventoResponseDTO buscar(@PathVariable Integer id) {
        return eventoService.buscar(id);
    }

    @PostMapping
    public ResponseEntity<EventoResponseDTO> criar(@Valid @RequestBody EventoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.criar(dto));
    }

    @PutMapping("/{id}")
    public EventoResponseDTO atualizar(@PathVariable Integer id,
                                       @Valid @RequestBody EventoRequestDTO dto) {
        return eventoService.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        eventoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
