package com.semac.java_api.dto;

/* Camiseta pedida na inscrição. `modelo` e `tamanho` são os nomes dos
   enums Modelo (NORMAL | BABY_LOOK) e Tamanho (PP | P | M | G | GG | XG | XXG).
   `avulsa` diz se foi comprada à parte (true) ou está inclusa no
   ingresso/kit (false) — editável no /admin por DIRETOR_SITE/PRESIDENTE. */
public record CamisetaParticipanteDTO(String modelo, String tamanho, Boolean avulsa) {}
