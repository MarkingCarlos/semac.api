package com.semac.java_api.dto;

/* Uma das palavras de um dia, como a diretoria a vê em /admin -> Termo.

   Sem a palavra, de propósito: nem quem cadastrou a lê de volta pela API.
   Mesmo critério de `codigoDefinido` no tipo de ingresso. O que volta é o
   suficiente para administrar a lista — o tamanho, para conferir que
   entrou o que se digitou, e quantos participantes a receberam no
   sorteio. Esquecer qual era significa remover e cadastrar de novo — o
   custo é baixo e fecha o caminho de descobrir a palavra por uma conta de
   diretoria.

   `jogadores` maior que zero = a palavra já foi sorteada para alguém e não
   pode mais ser removida. */
public record TermoPalavraAdminDTO(
        Integer id,
        int tamanho,
        long jogadores
) {}
