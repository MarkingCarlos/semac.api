package com.semac.java_api.dto;

import java.time.LocalDateTime;
import java.util.List;

/* `vagasRestantes` só faz sentido em evento que exige inscrição
   (minicurso) — vem null nos eventos abertos, cuja capacidade é folgada
   e em que todo participante confirmado já entra automaticamente.

   `iniciadoEm` é null até alguém apertar "iniciar" na lista de eventos do
   /admin; é o que faz a linha trocar o botão pela hora de início real. */
public record EventoResponseDTO(
        Integer id,
        String nome,
        TipoEventoResponseDTO tipoEvento,
        String local,
        String descricao,
        TrilhaResponseDTO trilha,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        LocalDateTime iniciadoEm,
        Integer capacidadeMaxima,
        Integer vagasRestantes,
        List<PalestranteDTO> palestrantes
) {}
