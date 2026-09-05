package com.semac.java_api.dto;

/* Uma camiseta no perfil do próprio usuário (seção Início do /admin) —
   igual a CamisetaParticipanteDTO, mas com o `id` do camisa_pedido, para
   o PATCH /api/pessoa/me casar cada linha editada com o registro certo
   (edição só; sem adicionar/remover — ver AtualizarPerfilDTO). */
public record CamisetaPerfilDTO(Integer id, String modelo, String tamanho, Boolean avulsa) {}
