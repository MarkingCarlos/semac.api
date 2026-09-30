package com.semac.java_api.dto;

import java.util.List;

/* Card de leitura de QR da dashboard. `eventosAgora` são os que estão com
   o check-in aberto neste momento (pode haver mais de um em paralelo);
   `eventos` são todos os que já abriram check-in, do mais recente ao mais
   antigo; `porMembro` soma as leituras de cada membro em todos eles. */
public record DashboardCheckinsDTO(
        List<DashboardEventoCheckinDTO> eventosAgora,
        List<DashboardEventoCheckinDTO> eventos,
        List<DashboardLeiturasMembroDTO> porMembro,
        long totalLeituras
) {}
