package com.semac.java_api.dto;

/* Perfil do próprio usuário autenticado — usado pela seção "Início" do
   /admin. Expõe só o necessário para exibir e editar: dados de conta
   (nome, email, role — read-only), o RA e a camiseta (editáveis).
   Nunca expõe senha/cpf/uuid. `camiseta` pode ser null se, por algum
   motivo, não houver pedido registrado. */
public record PerfilResponseDTO(
        Integer id,
        String nome,
        String email,
        String role,
        String ra,
        CamisetaParticipanteDTO camiseta
) {}
