package com.semac.java_api.dto;

import java.util.List;

/* O quiz de um laboratório como o participante o vê ao ler o QR
   (GET /api/laboratorio/quiz/<codigo>). As alternativas vêm SEM a marca
   de correta: a correção é só no servidor.

   `resultado` vem preenchido se a pessoa já respondeu — a tela mostra o
   placar em vez das perguntas. `quizAberto` false: a comissão ainda não
   liberou (ou já fechou), e as perguntas vêm vazias. */
public record QuizLaboratorioDTO(
        String nome,
        String responsaveis,
        boolean quizAberto,
        List<Pergunta> perguntas,
        ResultadoQuizLaboratorioDTO resultado
) {
    public record Pergunta(Integer id, String enunciado, List<Alternativa> alternativas) {}

    public record Alternativa(Integer id, String texto) {}
}
