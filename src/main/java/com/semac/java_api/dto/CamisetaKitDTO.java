package com.semac.java_api.dto;

/* Camiseta a entregar no kit (sub-aba Kits do /admin). `publico` diz de
   qual pilha ela sai: "COMISSAO" só para a inclusa no kit de quem é da
   comissão; "PARTICIPANTE" para o resto, inclusive avulsa comprada por
   diretor — mesma regra do relatório de camisetas
   (CamisaPedidoRepository.consultarEstoqueComissao). */
public record CamisetaKitDTO(String modelo, String tamanho, String publico, Boolean avulsa) {}
