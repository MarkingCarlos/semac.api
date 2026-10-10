package com.semac.java_api.dto;

import java.time.LocalDateTime;

/* Metadados de um comprovante anexado a um item da previsão. O arquivo em
   si sai por GET /api/previsao/{id}/comprovantes/{comprovanteId}. */
public record ComprovantePrevisaoResponseDTO(
        Integer id,
        String nomeOriginal,
        String tipoConteudo,
        Long tamanhoBytes,
        LocalDateTime enviadoEm
) {}
