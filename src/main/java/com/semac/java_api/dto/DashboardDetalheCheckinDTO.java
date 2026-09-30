package com.semac.java_api.dto;

import java.util.List;

/* Detalhe de um evento no modal de leitura de QR: quantas leituras cada
   membro fez nele e a lista de quem leu quem, da mais recente à mais
   antiga. */
public record DashboardDetalheCheckinDTO(
        DashboardEventoCheckinDTO evento,
        List<DashboardLeiturasMembroDTO> porMembro,
        List<DashboardLeituraQrDTO> leituras
) {}
