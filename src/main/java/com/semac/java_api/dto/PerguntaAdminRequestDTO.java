package com.semac.java_api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/* Pergunta incluída à mão pela diretoria/presidência no /admin
   (POST /api/evento/{id}/perguntas). O debate vem da URL; `nomeAutor` é
   quem fez a pergunta (opcional — vazio aparece como "Anônimo").
   `daMediacao` true = pergunta da mediação, criada no roteiro do painel
   do debate, já no `bloco` informado (opcional). */
public record PerguntaAdminRequestDTO(
        @NotBlank(message = "Escreva a pergunta.")
        @Size(max = 500, message = "A pergunta pode ter no máximo 500 caracteres.")
        String texto,

        @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres.")
        String nomeAutor,

        @Min(value = 1, message = "Bloco inválido.")
        @Max(value = PerguntaBlocoRequestDTO.MAXIMO_BLOCOS, message = "Bloco inválido.")
        Integer bloco,

        Boolean daMediacao
) {}
