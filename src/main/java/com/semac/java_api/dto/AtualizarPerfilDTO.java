package com.semac.java_api.dto;

/* Campo que o usuário pode alterar no próprio perfil (seção Início do
   /admin): só o RA. As camisetas já foram encomendadas e não são mais
   editáveis. */
public record AtualizarPerfilDTO(
        String ra
) {}
