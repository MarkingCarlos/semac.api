package com.semac.java_api.dto;

import java.util.List;

/* Visão de um participante para a tabela do /admin. Não expõe dados
   sensíveis (senha, cpf, uuid) — apenas o necessário para a listagem.
   `role` é null enquanto a inscrição aguarda confirmação.
   `camiseta` é a primeira camiseta pedida (null se não houver nenhuma) e
   `camisetas` traz todas — quem escolhe um ingresso com mais de uma grátis,
   ou compra avulsas, tem vários pedidos.
   `tipoInscricao` é o ingresso escolhido no cadastro, podendo ser trocado
   na confirmação; `dias` acompanha ingresso de diária.
   `nivel`/`xp` são atribuídos na confirmação (null se pendente).
   `temComprovante` diz se a pessoa anexou comprovante de pagamento no
   cadastro (o arquivo em si vem de GET /api/pessoa/{id}/comprovante).
   `formaPagamento` é "PIX", "CARTAO" ou null (ainda não pagou nada);
   `pagamentoCartao` só vem preenchido quando formaPagamento = "CARTAO" —
   é a evidência que substitui o comprovante nesse caso. */
public record ParticipanteResponseDTO(
        Integer id,
        String nome,
        String email,
        String ra,
        Boolean ativo,
        String role,
        CamisetaParticipanteDTO camiseta,
        List<CamisetaParticipanteDTO> camisetas,
        TipoInscricaoResponseDTO tipoInscricao,
        Integer dias,
        NivelResponseDTO nivel,
        Integer xp,
        List<PresencaParticipanteDTO> eventoParticipantes,
        Boolean temComprovante,
        String formaPagamento,
        PagamentoCartaoInfoDTO pagamentoCartao
) {}
