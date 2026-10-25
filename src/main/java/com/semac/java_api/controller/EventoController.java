package com.semac.java_api.controller;

import com.semac.java_api.dto.EventoRequestDTO;
import com.semac.java_api.dto.EventoResponseDTO;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evento")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    /* GET aberto: alimenta a programação pública. Os campos de operação
       saem conforme quem pede (ver paraQuemPede). */
    @GetMapping
    public List<EventoResponseDTO> listar(@AuthenticationPrincipal Jwt jwt) {
        return eventoService.listar().stream()
                .map(evento -> paraQuemPede(evento, jwt))
                .toList();
    }

    @GetMapping("/{id}")
    public EventoResponseDTO buscar(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        return paraQuemPede(eventoService.buscar(id), jwt);
    }

    /* A comissão recebe tudo. O participante recebe também lotação e vagas,
       que a escolha de minicursos usa. O visitante (sem token, jwt null)
       fica sem os dois e sem os campos de operação do evento: início real
       e timer do telão do debate. */
    private EventoResponseDTO paraQuemPede(EventoResponseDTO evento, Jwt jwt) {
        String role = jwt == null ? null : jwt.getClaimAsString("role");
        if (role != null && !Role.PARTICIPANTE.name().equals(role)) return evento;

        boolean participante = Role.PARTICIPANTE.name().equals(role);
        return new EventoResponseDTO(
                evento.id(),
                evento.nome(),
                evento.tipoEvento(),
                evento.local(),
                evento.descricao(),
                evento.trilha(),
                evento.dataHoraInicio(),
                evento.dataHoraFim(),
                null,
                participante ? evento.capacidadeMaxima() : null,
                participante ? evento.vagasRestantes() : null,
                evento.palestrantes(),
                null,
                null,
                null
        );
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
