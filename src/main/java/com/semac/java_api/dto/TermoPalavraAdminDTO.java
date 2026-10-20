package com.semac.java_api.dto;

/* Uma das palavras de um dia, como a diretoria a vê em /admin -> Termo.

   Traz o texto da palavra para a diretoria poder conferir o que cadastrou.
   Só sai por GET /api/termo/dias, restrito a PAPEIS_GAMIFICACAO (ver
   SecurityConfig) — nenhuma rota do participante devolve este DTO. Na tela
   ela vem mascarada e só aparece com "Mostrar", para não vazar num
   compartilhamento de tela.

   `jogadores` maior que zero = a palavra já foi sorteada para alguém e não
   pode mais ser removida. */
public record TermoPalavraAdminDTO(
        Integer id,
        String palavra,
        int tamanho,
        long jogadores
) {}
