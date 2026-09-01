package com.semac.java_api.dto;

import com.semac.java_api.model.enums.Modelo;
import com.semac.java_api.model.enums.Tamanho;
import jakarta.validation.constraints.NotNull;

/* Uma camiseta no editor administrativo do /admin (abas Participantes e
   Comissão) — igual a CamisetaPedidoDTO, mas com o avulsa explícito, que só
   quem edita (DIRETOR_SITE/PRESIDENTE) define. */
public record CamisetaAdminDTO(
        @NotNull Modelo modelo,
        @NotNull Tamanho tamanho,
        @NotNull Boolean avulsa
) {}
