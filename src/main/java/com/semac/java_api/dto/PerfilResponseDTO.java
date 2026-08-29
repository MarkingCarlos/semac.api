package com.semac.java_api.dto;

/* Perfil do próprio usuário autenticado — usado pela seção "Início" do
   /admin e pela área /participantes (nível/xp). Expõe dados de conta
   (nome, email, role — read-only), o RA e a camiseta (editáveis), e a
   gamificação de quem é PARTICIPANTE. Nunca expõe senha/cpf/uuid.
   `camiseta` pode ser null se, por algum motivo, não houver pedido
   registrado. Os campos de xp/nível/ranking vêm null para quem não é
   PARTICIPANTE (comissão) ou ainda não tem xp atribuído; `nivel` vem
   preenchido mas `proximoNivelNome`/`xpFaltanteProximoNivel` vêm null
   quando a pessoa já está no nível mais alto cadastrado. */
public record PerfilResponseDTO(
        Integer id,
        String nome,
        String email,
        String role,
        String ra,
        CamisetaParticipanteDTO camiseta,
        Integer xp,
        NivelResponseDTO nivel,
        String proximoNivelNome,
        Integer xpFaltanteProximoNivel,
        Integer posicaoRanking,
        Integer totalParticipantesRanking
) {}
