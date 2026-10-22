package com.semac.java_api.controller;

import com.semac.java_api.dto.ProfessorConvidadoRequestDTO;
import com.semac.java_api.dto.ProfessorConvidadoResponseDTO;
import com.semac.java_api.model.ProfessorConvidado;
import com.semac.java_api.repository.ProfessorConvidadoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

/* CRUD dos professores convidados (tabela `professor_convidado`).
   Gerenciado na subaba Crachás da aba Pessoas do /admin, que imprime o
   crachá deles com a arte própria de professor.
   Restrito no SecurityConfig ao mesmo público de /api/cracha. */
@RestController
@RequestMapping("/api/professor-convidado")
public class ProfessorConvidadoController {

    private final ProfessorConvidadoRepository professorConvidadoRepository;

    public ProfessorConvidadoController(ProfessorConvidadoRepository professorConvidadoRepository) {
        this.professorConvidadoRepository = professorConvidadoRepository;
    }

    @GetMapping
    public List<ProfessorConvidadoResponseDTO> listar() {
        return professorConvidadoRepository.findAll().stream()
                .sorted(Comparator.comparing(ProfessorConvidado::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(this::paraResposta)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ProfessorConvidadoResponseDTO> criar(@Valid @RequestBody ProfessorConvidadoRequestDTO dto) {
        ProfessorConvidado professor = new ProfessorConvidado();
        professor.setNome(dto.nome().trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(paraResposta(professorConvidadoRepository.save(professor)));
    }

    @PutMapping("/{id}")
    public ProfessorConvidadoResponseDTO atualizar(@PathVariable Integer id,
                                                   @Valid @RequestBody ProfessorConvidadoRequestDTO dto) {
        ProfessorConvidado professor = professorConvidadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado."));
        professor.setNome(dto.nome().trim());
        return paraResposta(professorConvidadoRepository.save(professor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Integer id) {
        if (!professorConvidadoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        professorConvidadoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ProfessorConvidadoResponseDTO paraResposta(ProfessorConvidado professor) {
        return new ProfessorConvidadoResponseDTO(professor.getId(), professor.getNome());
    }
}
