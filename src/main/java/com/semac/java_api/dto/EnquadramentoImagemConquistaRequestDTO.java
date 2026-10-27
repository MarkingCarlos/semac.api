package com.semac.java_api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/* Enquadramento da imagem da conquista na moldura dos cards (botão
   "Ajustar imagem" do /admin -> Informações SEMAC): ponto em evidência,
   em % da imagem, e zoom. Mesmos limites do enquadramento da foto do
   palestrante (PalestranteDTO). */
public record EnquadramentoImagemConquistaRequestDTO(
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal imagemPosX,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal imagemPosY,
        @NotNull @DecimalMin("1") @DecimalMax("3") BigDecimal imagemZoom
) {}
