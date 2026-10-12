package com.semac.java_api.dto;

/* Um time como o /admin (aba Times) enxerga: cadastro editável mais a
   contagem de integrantes e de quem já abriu /time. */
public record TimeAdminDTO(
        Integer id,
        String nome,
        String cor,
        String mascoteVersao,
        long integrantes,
        long revelados,
        long xpTime
) {}
