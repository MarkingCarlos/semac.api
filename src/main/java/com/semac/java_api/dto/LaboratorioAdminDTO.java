package com.semac.java_api.dto;

import java.util.List;

/* Um laboratório como a comissão o vê no /admin: com as perguntas (e qual
   alternativa é a correta, para a edição) e os contadores. `codigoQr` é o
   que a tela usa para montar o link /laboratorio/<codigoQr>. */
public record LaboratorioAdminDTO(
        Integer id,
        String nome,
        String responsaveis,
        String codigoQr,
        boolean quizAberto,
        List<Pergunta> perguntas,
        long respostas,
        long gabaritos
) {
    public record Pergunta(Integer id, String enunciado, List<Alternativa> alternativas) {}

    public record Alternativa(Integer id, String texto, boolean correta) {}
}
