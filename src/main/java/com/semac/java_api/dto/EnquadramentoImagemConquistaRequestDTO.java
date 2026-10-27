package com.semac.java_api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/* Enquadramento da imagem da conquista na moldura dos cards (botão
   "Ajustar imagem" do /admin -> Informações SEMAC): ponto em evidência,
   em % da imagem, e zoom. A posição tem os mesmos limites da foto do
   palestrante (PalestranteDTO); o zoom desce até 0,3 porque aqui dá para
   diminuir a imagem dentro da moldura (a foto do telão sempre a cobre). */
public record EnquadramentoImagemConquistaRequestDTO(
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal imagemPosX,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal imagemPosY,
        @NotNull @DecimalMin("0.3") @DecimalMax("3") BigDecimal imagemZoom
) {}
