package com.semac.java_api.dto;

import com.semac.java_api.model.enums.Modelo;
import com.semac.java_api.model.enums.Tamanho;
import jakarta.validation.constraints.NotNull;

/* Uma camiseta pedida no cadastro. A lista enviada em InscricaoRequestDTO
   mistura as gratuitas do ingresso e as avulsas compradas — o banco não
   distingue as duas (ver camisa_pedido). */
public record CamisetaPedidoDTO(
        @NotNull Modelo modelo,
        @NotNull Tamanho tamanho
) {}
