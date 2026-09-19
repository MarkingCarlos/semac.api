package com.semac.java_api.dto;

import java.time.LocalDateTime;

/* Resposta do botão "iniciar" da lista de eventos do /admin. `iniciadoEm`
   é sempre o valor vigente — em um segundo clique, volta o horário do
   primeiro, que é o que a linha passa a exibir. */
public record EventoIniciadoDTO(
        Integer eventoId,
        LocalDateTime iniciadoEm
) {}
