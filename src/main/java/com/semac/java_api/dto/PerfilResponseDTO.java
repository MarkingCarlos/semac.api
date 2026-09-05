package com.semac.java_api.dto;

import java.util.List;

/* Perfil do próprio usuário autenticado — usado pela seção "Início" do
   /admin e pela área /participantes (nível/xp). Expõe dados de conta
   (nome, email, role — read-only), o RA e as camisetas (editáveis), e a
   gamificação de quem é PARTICIPANTE. Nunca expõe senha/cpf/uuid.
   `camisetas` traz todos os pedidos da pessoa (uma pessoa pode ter mais
   de um — a inclusa no kit e eventuais avulsas), vazia se não houver
   nenhum registrado. Os campos de xp/nível/ranking vêm null para quem não é
   PARTICIPANTE (comissão) ou ainda não tem xp atribuído; `nivel` vem
   preenchido mas `proximoNivelNome`/`xpFaltanteProximoNivel` vêm null
   quando a pessoa já está no nível mais alto cadastrado. */
public record PerfilResponseDTO(
        Integer id,
        String nome,
        String email,
        String role,
        String ra,
        List<CamisetaPerfilDTO> camisetas,
        Integer xp,
        NivelResponseDTO nivel,
        String proximoNivelNome,
        Integer xpFaltanteProximoNivel,
        Integer posicaoRanking,
        Integer totalParticipantesRanking
) {}
