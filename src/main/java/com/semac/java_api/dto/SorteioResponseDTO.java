package com.semac.java_api.dto;

import java.time.LocalDateTime;

public record SorteioResponseDTO(
        Integer id,
        String participanteNome,
        LocalDateTime realizadoEm
) {}
