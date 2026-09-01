package com.semac.java_api.dto;

import com.semac.java_api.model.enums.Modelo;
import com.semac.java_api.model.enums.Tamanho;
import jakarta.validation.constraints.NotNull;

/* Uma camiseta pedida no cadastro. A lista enviada em InscricaoRequestDTO
   traz as gratuitas do ingresso primeiro, depois as avulsas compradas —
   InscricaoService usa essa ordem para marcar o campo `avulsa` de cada
   camisa_pedido ao salvar. */
public record CamisetaPedidoDTO(
        @NotNull Modelo modelo,
        @NotNull Tamanho tamanho
) {}
