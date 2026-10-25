package com.semac.java_api.dto;

import java.util.Map;

/* Estado do telão de um debate (ver TelaoDebateService).
   `estado` null e `alteradoEm` 0 = ninguém publicou ainda.
   `servidorAgora` é a hora do servidor na resposta: cada aparelho mede a
   diferença para o próprio relógio e converte o timer, senão celular e
   notebook mostrariam tempos diferentes.
   `telaoVistoEm` é a última consulta da guia projetada (null se nunca). */
public record TelaoDebateResponseDTO(
        Map<String, Object> estado,
        long alteradoEm,
        long servidorAgora,
        Long telaoVistoEm
) {}
