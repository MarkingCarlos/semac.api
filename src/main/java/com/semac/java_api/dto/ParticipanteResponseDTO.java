package com.semac.java_api.dto;

import java.util.List;

/* Visão de um participante para a tabela do /admin. Não expõe dados
   sensíveis (senha, cpf, uuid) — apenas o necessário para a listagem.
   `role` é null enquanto a inscrição aguarda confirmação.
   `camiseta` pode ser null se a pessoa não tiver pedido registrado.
   `tipoInscricao` é o ingresso escolhido na confirmação (null se pendente). */
public record ParticipanteResponseDTO(
        Integer id,
        String nome,
        String email,
        String ra,
        Boolean ativo,
        String role,
        CamisetaParticipanteDTO camiseta,
        TipoInscricaoResponseDTO tipoInscricao,
        List<PresencaParticipanteDTO> eventoParticipantes
) {}
