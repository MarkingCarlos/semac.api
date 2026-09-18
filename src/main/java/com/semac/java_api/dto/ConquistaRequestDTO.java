package com.semac.java_api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/* Campos do catálogo que a presidência edita em /admin -> Informações
   SEMAC. `codigo` e `tipoValidacao` ficam de fora de propósito: pertencem
   ao código (ver CatalogoConquistas) e são sincronizados no boot pelo
   ConquistaSeedRunner. A imagem vai por upload, em rota própria, e `ativa`
   por PATCH — ativar/desativar tem regra própria e não deve viajar junto
   de uma edição de texto. */
public record ConquistaRequestDTO(
        @NotBlank(message = "O nome da conquista é obrigatório.")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
        String nome,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String descricao,

        /* É exatamente o xp creditado — não há multiplicação por raridade. */
        @NotNull(message = "Os pontos são obrigatórios.")
        @Min(value = 0, message = "Os pontos não podem ser negativos.")
        @Max(value = 10000, message = "Os pontos devem ser no máximo 10000.")
        Integer pontosBase,

        /* Só o tier visual do card (1 comum a 5 lendária). */
        @NotNull(message = "A raridade é obrigatória.")
        @Min(value = 1, message = "A raridade vai de 1 a 5.")
        @Max(value = 5, message = "A raridade vai de 1 a 5.")
        Integer raridade,

        @NotNull(message = "A ordem é obrigatória.")
        @Min(value = 0, message = "A ordem não pode ser negativa.")
        Integer ordem
) {}
