package com.semac.java_api.dto;

import java.time.LocalDate;
import java.util.List;

/* Um desafio como a diretoria o vê em /admin -> Cifra do Dia, com as
   respostas — a rota é só de PAPEIS_GAMIFICACAO, e a tela as mostra
   mascaradas até alguém clicar em "Mostrar", como no Termo. */
public record DesafioCifraAdminDTO(
        Integer id,
        Integer ano,
        LocalDate data,
        String palestra,
        String senhaCifrada,
        List<DesafioCifraEtapaAdminDTO> etapas,
        long jogadores,
        long vencedores
) {}
