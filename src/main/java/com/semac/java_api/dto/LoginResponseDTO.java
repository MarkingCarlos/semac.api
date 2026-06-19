package com.semac.java_api.dto;

/* Resposta do login: o token Bearer (JWT) que o cliente envia em cada
   requisição protegida, mais os dados básicos do usuário (sem expor
   senha/cpf/uuid). `role` pode ser null enquanto a inscrição aguarda
   confirmação. */
public record LoginResponseDTO(
        String token,
        Integer id,
        String nome,
        String email,
        String role
) {}
