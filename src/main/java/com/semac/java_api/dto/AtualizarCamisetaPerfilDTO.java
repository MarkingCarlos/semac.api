package com.semac.java_api.dto;

import com.semac.java_api.model.enums.Modelo;
import com.semac.java_api.model.enums.Tamanho;
import jakarta.validation.constraints.NotNull;

/* Uma linha do PATCH /api/pessoa/me: edita modelo/tamanho de uma camiseta
   já existente da própria pessoa. `id` é obrigatório e precisa bater com
   um camisa_pedido dela — essa rota nunca cria nem apaga camiseta (isso é
   exclusivo do editor do admin, PUT /api/pessoa/{id}/camisetas), nem
   deixa a pessoa alterar `avulsa`. */
public record AtualizarCamisetaPerfilDTO(
        @NotNull Integer id,
        @NotNull Modelo modelo,
        @NotNull Tamanho tamanho
) {}
