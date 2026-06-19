package com.semac.java_api.controller;

import com.semac.java_api.dto.AtivoRequestDTO;
import com.semac.java_api.dto.AtribuirRoleDTO;
import com.semac.java_api.dto.InscricaoFinanceiraDTO;
import com.semac.java_api.dto.ParticipanteResponseDTO;
import com.semac.java_api.service.PessoaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pessoa")
public class PessoaController {

    private final PessoaService pessoaService;

    public PessoaController(PessoaService pessoaService) {
        this.pessoaService = pessoaService;
    }

    /* Lista para a tabela de participantes do /admin (confirmados + pendentes). */
    @GetMapping("/participantes")
    public List<ParticipanteResponseDTO> listarParticipantes() {
        return pessoaService.listarParticipantes();
    }

    /* Lista a comissão organizadora do /admin (papéis != PARTICIPANTE). */
    @GetMapping("/comissao")
    public List<ParticipanteResponseDTO> listarComissao() {
        return pessoaService.listarComissao();
    }

    /* Inscrições confirmadas para o módulo financeiro (somente leitura). */
    @GetMapping("/inscricoes")
    public List<InscricaoFinanceiraDTO> listarInscricoes() {
        return pessoaService.listarInscricoes();
    }

    /* Confirma a inscrição atribuindo o papel (PARTICIPANTE ou comissão). */
    @PatchMapping("/{id}/role")
    public ParticipanteResponseDTO atribuirRole(@PathVariable Integer id,
                                                @Valid @RequestBody AtribuirRoleDTO dto) {
        return pessoaService.atribuirRole(id, dto.role(), dto.tipoInscricaoId());
    }

    /* Ativa/desativa uma pessoa (ex.: suspender membro da comissão). */
    @PatchMapping("/{id}/ativo")
    public ParticipanteResponseDTO definirAtivo(@PathVariable Integer id,
                                                @Valid @RequestBody AtivoRequestDTO dto) {
        return pessoaService.definirAtivo(id, dto.ativo());
    }
}
