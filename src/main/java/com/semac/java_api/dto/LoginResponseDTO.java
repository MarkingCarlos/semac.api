package com.semac.java_api.dto;

/* Resposta do login: o token Bearer (JWT) que o cliente envia em cada
   requisição protegida, mais os dados básicos do usuário (sem expor
   senha/cpf). `role` pode ser null enquanto a inscrição aguarda
   confirmação. `uuid` é o identificador estável usado pra gerar o QR
   code pessoal do participante (ver /participantes) — seguro de expor
   ao próprio dono da sessão, já que sozinho não concede nada sem
   passar por um endpoint de verificação autenticado. */
public record LoginResponseDTO(
        String token,
        Integer id,
        String nome,
        String email,
        String role,
        String uuid
) {}
