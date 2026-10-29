package com.semac.java_api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/* Um desafio (Termo ou Cifra do Dia) que o participante acertou, como a
   página de resumo do /admin mostra. `titulo` é "Dia N" no Termo e o nome
   da palestra na Cifra. A palavra do Termo não sai daqui — mesmo critério
   de TermoPalavra, que nunca a expõe na API. */
public record DesafioVencidoResumoParticipanteDTO(
        Integer id,
        String titulo,
        LocalDate data,
        LocalDateTime acertadoEm,
        Integer tentativasUsadas,
        Integer xpCreditado
) {}
