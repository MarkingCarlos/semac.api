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
   `nivel`/`xp` são atribuídos na confirmação (null se pendente). */
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
        List<PresencaParticipanteDTO> eventoParticipantes
) {}
