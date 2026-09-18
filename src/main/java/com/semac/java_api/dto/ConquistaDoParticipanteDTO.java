package com.semac.java_api.dto;

import java.time.LocalDateTime;

/* Uma conquista que um participante possui, como o /admin a vê no painel
   de revogação. `xpCreditado` é o que será estornado se for revogada, e
   `concedidaPorNome` vem null nas automáticas — ali quem concedeu foi o
   sistema. */
public record ConquistaDoParticipanteDTO(
        Integer conquistaId,
        String nome,
        String tipoValidacao,
        Integer xpCreditado,
        LocalDateTime obtidaEm,
        String concedidaPorNome
) {}
