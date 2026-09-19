package com.semac.java_api.dto;

/* Uma linha da lista de regras de xp (GET /api/regra-xp). Vem de duas
   tabelas: `tipo_evento` (origem TIPO_EVENTO, chave "TIPO_EVENTO:<id>")
   e `regra_xp` (origem REGRA). `unidade` é PONTOS ou MINUTOS — ver
   UnidadeRegraXp. A chave é o que volta no PUT. */
public record RegraXpResponseDTO(
        String chave,
        String nome,
        Integer valor,
        String unidade,
        String origem
) {}
