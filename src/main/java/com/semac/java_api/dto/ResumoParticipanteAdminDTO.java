package com.semac.java_api.dto;

import java.util.List;

/* Resumo de um participante para a página /admin/participante/{id}:
   pontos, time, conquistas e presenças numa chamada só.
   `time` é null enquanto os times não foram sorteados (ou a pessoa ficou
   de fora do sorteio). `presencas` traz só os check-ins confirmados
   (status PRESENTE), do mais recente para o mais antigo.
   `termosVencidos` e `cifrasVencidas` trazem só os desafios acertados,
   também do mais recente para o mais antigo. */
public record ResumoParticipanteAdminDTO(
        Integer id,
        String nome,
        String email,
        Integer xp,
        NivelResponseDTO nivel,
        TimeResumoParticipanteDTO time,
        List<ConquistaDoParticipanteDTO> conquistas,
        List<PresencaResumoParticipanteDTO> presencas,
        List<DesafioVencidoResumoParticipanteDTO> termosVencidos,
        List<DesafioVencidoResumoParticipanteDTO> cifrasVencidas
) {}
