package com.semac.java_api.dto;

/* Configuração de inscrições da edição. `id` vem null enquanto o ano não
   foi configurado — nesse caso `inscricoesAbertas` é true (comportamento
   padrão: botão visível). */
public record ConfiguracaoInscricaoResponseDTO(
        Integer id,
        Integer ano,
        Boolean inscricoesAbertas
) {}
