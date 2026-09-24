package com.semac.java_api.dto;

/* Uma pessoa a receber crachá impresso (aba Crachás do /admin).

   `perfil`: PARTICIPANTE (confirmado ou pendente), COMISSAO ou
   PALESTRANTE. `uuid` é o mesmo do crachá digital e vira o QR de
   check-in; palestrante não tem uuid (tabela à parte, sem check-in), então
   vem null e o crachá sai sem QR. */
public record CrachaDTO(
        Integer id,
        String nome,
        String uuid,
        String perfil
) {}
