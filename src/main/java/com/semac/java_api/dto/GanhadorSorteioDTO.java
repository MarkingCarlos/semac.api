package com.semac.java_api.dto;

import java.time.LocalDateTime;

/* Uma entrega registrada: quem ganhou, qual brinde e quem realizou o sorteio. */
public record GanhadorSorteioDTO(
        Integer id,
        String brindeNome,
        String participanteNome,
        String organizadorNome,
        LocalDateTime ganhouEm
) {}
