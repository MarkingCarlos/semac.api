package com.semac.java_api.controller;

import com.semac.java_api.dto.EventoResponseDTO;
import com.semac.java_api.dto.MeuEventoResponseDTO;
import com.semac.java_api.model.EventoParticipante;
import com.semac.java_api.service.EventoService;
import com.semac.java_api.service.InscricaoEventoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/* Agenda do próprio participante e escolha de minicursos, usadas pela
   área /participantes. O participante é sempre identificado pela claim
   `id` do Bearer token — nunca por parâmetro, para ninguém se inscrever
   no lugar de outra pessoa.

   Eventos abertos não passam por aqui: o participante já é pré-inscrito
   neles ao ter a inscrição confirmada (ver InscricaoEventoService). */
@RestController
@RequestMapping("/api/evento")
public class InscricaoEventoController {

    private final InscricaoEventoService inscricaoEventoService;
    private final EventoService eventoService;

    public InscricaoEventoController(InscricaoEventoService inscricaoEventoService,
                                     EventoService eventoService) {
        this.inscricaoEventoService = inscricaoEventoService;
        this.eventoService = eventoService;
    }

    /* Eventos do participante logado (palestras pré-inscritas + minicursos
       escolhidos), ordenados por horário. */
    @GetMapping("/meus")
    public List<MeuEventoResponseDTO> meusEventos(@AuthenticationPrincipal Jwt jwt) {
        List<EventoParticipante> inscricoes = inscricaoEventoService
                .listarInscricoesDoParticipante(idDoToken(jwt)).stream()
                .sorted(Comparator.comparing(inscricao -> inscricao.getEvento().getDataHoraInicio()))
                .toList();

        /* Uma consulta de ocupação para a lista toda (as vagas restantes
           dos minicursos), em vez de uma por evento. */
        List<EventoResponseDTO> eventos = eventoService.montarRespostas(
                inscricoes.stream().map(EventoParticipante::getEvento).toList());

        return IntStream.range(0, inscricoes.size())
                .mapToObj(i -> new MeuEventoResponseDTO(eventos.get(i), inscricoes.get(i).getStatus().name()))
                .toList();
    }

    /* Entra em um minicurso. Conflitos de regra (esgotado, choque de
       horário, já inscrito) voltam como 409 com {mensagem}. */
    @PostMapping("/{id}/inscricao")
    public ResponseEntity<Void> inscrever(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        inscricaoEventoService.inscrever(idDoToken(jwt), id);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /* Desiste de um minicurso, liberando a vaga. */
    @DeleteMapping("/{id}/inscricao")
    public ResponseEntity<Void> cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        inscricaoEventoService.cancelar(idDoToken(jwt), id);
        return ResponseEntity.noContent().build();
    }

    /* Extrai o id da pessoa da claim `id` do token (gravada no login). */
    private Integer idDoToken(Jwt jwt) {
        Object id = jwt == null ? null : jwt.getClaim("id");
        if (id instanceof Number numero) {
            return numero.intValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
    }
}
