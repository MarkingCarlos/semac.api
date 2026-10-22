package com.semac.java_api.dto;

import java.time.LocalDateTime;
import java.util.List;

/* Uma pessoa na sub-aba Kits do /admin. Enxuto de propósito: a tela é
   aberta a toda a comissão, então não leva e-mail, telefone, RA nem nada
   de pagamento — só o que ajuda a achar a pessoa e montar o kit.
   `role` é PARTICIPANTE ou o papel na comissão; `ingresso` vem null para
   a comissão. `kitEntregueEm` null = kit ainda não entregue;
   `kitEntreguePorNome` é null também quando quem entregou foi excluído. */
public record KitPessoaDTO(
        Integer id,
        String nome,
        String role,
        String ingresso,
        List<CamisetaParticipanteDTO> camisetas,
        LocalDateTime kitEntregueEm,
        String kitEntreguePorNome
) {}
