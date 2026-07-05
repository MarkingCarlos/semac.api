package com.semac.java_api.dto;

import com.semac.java_api.model.enums.Modelo;
import com.semac.java_api.model.enums.Tamanho;
import jakarta.validation.constraints.NotNull;

/* Campos que o usuário pode alterar no próprio perfil (seção Início):
   RA (opcional) e a camiseta (modelo + tamanho, obrigatórios). Modelo e
   Tamanho chegam como os nomes dos enums (ex.: "BABY_LOOK", "M"); um
   valor inválido é rejeitado com 400 já na desserialização. */
public record AtualizarPerfilDTO(
        String ra,
        @NotNull(message = "Selecione o tipo da camiseta.") Modelo modelo,
        @NotNull(message = "Selecione o tamanho da camiseta.") Tamanho tamanho
) {}
