package com.semac.java_api.dto;

import java.time.LocalDate;
import java.util.List;

/* Um dos quatro dias, como a diretoria o vê em /admin -> Termo.

   Dia ainda não cadastrado vem com `data` null e lista vazia, para a tela
   sempre ter os quatro cards. As palavras vêm com o texto, só para a
   diretoria — ver TermoPalavraAdminDTO. */
public record TermoDiaAdminDTO(
        Integer dia,
        LocalDate data,
        List<TermoPalavraAdminDTO> palavras
) {}
