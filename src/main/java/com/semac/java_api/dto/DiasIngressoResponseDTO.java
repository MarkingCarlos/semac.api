package com.semac.java_api.dto;

import java.time.LocalDate;
import java.util.List;

/* Estado da escolha de dias do ingresso diário (área /participantes).

   - `porDia`: false para quem tem ingresso de valor fixo — aí o resto vem
     vazio e a tela não pede escolha nenhuma.
   - `diasContratados`: quantas diárias a pessoa pagou (pessoa.dias_inscricao).
   - `diasDisponiveis`: dias com algum evento na programação.
   - `diasEscolhidos`: os que a pessoa já marcou.
   - `diasTravados`: escolhidos que não podem mais sair (hoje ou passados). */
public record DiasIngressoResponseDTO(
        boolean porDia,
        int diasContratados,
        List<LocalDate> diasDisponiveis,
        List<LocalDate> diasEscolhidos,
        List<LocalDate> diasTravados
) {}
