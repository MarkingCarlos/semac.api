package com.semac.java_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/* Corpo do POST /api/pessoa — cadastro manual feito pela comissão no
   /admin (inscrição de balcão: pagamento em dinheiro, cortesia, alguém
   que se inscreveu presencialmente).

   Difere do cadastro público (InscricaoRequestDTO) em dois pontos:
   `camisetas` usa CamisetaAdminDTO, com o `avulsa` explícito — no balcão
   é o organizador quem decide o que é inclusa no kit e o que foi comprado
   à parte; e `confirmar` diz se a pessoa já entra confirmada
   (role = PARTICIPANTE, com xp/nível e pré-inscrição nos eventos abertos)
   ou se vai para a fila de pendentes (role = NULL), como quem se inscreve
   pelo site.

   A senha é definida por quem cadastra e repassada à pessoa — é com ela
   que dá pra entrar na área /participantes depois. */
public record CadastroManualRequestDTO(
        @NotBlank String nome,
        @NotBlank String cpf,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String senha,
        String ra,
        @NotBlank @Pattern(regexp = "\\d{10,11}", message = "Telefone inválido.") String telefone,
        boolean ehUnesp,
        @NotNull Integer tipoInscricaoId,
        @Min(1) Integer dias,
        @Valid List<CamisetaAdminDTO> camisetas,
        boolean confirmar
) {}
