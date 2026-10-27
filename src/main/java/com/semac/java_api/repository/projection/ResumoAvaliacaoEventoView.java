package com.semac.java_api.repository.projection;

/* Quantas notas um evento recebeu e a média delas (ver
   AvaliacaoEventoRepository.resumirPorEvento). Uma query só para o
   resumo do /admin, sem N+1. */
public interface ResumoAvaliacaoEventoView {
    Integer getEventoId();
    Long getTotal();
    Double getMedia();
}
