package com.semac.java_api.config;

import com.semac.java_api.model.enums.TipoValidacaoConquista;

import java.util.List;

import static com.semac.java_api.model.enums.TipoValidacaoConquista.AUTOMATICA;
import static com.semac.java_api.model.enums.TipoValidacaoConquista.MANUAL;

/* Toda conquista que existe no sistema está declarada aqui. Esta lista é a
   fonte da verdade do catálogo: o ConquistaSeedRunner garante que cada
   entrada tenha sua linha na tabela `conquista`, e a presidência configura
   o resto em /admin -> Informações SEMAC.

   Adicionar uma conquista nova = uma entrada aqui + (se for AUTOMATICA) um
   método de regra em ConquistaService chamado do ponto certo. Nada de
   motor de regras genérico — mesmo padrão já usado nas regras de xp (ver
   InscricaoEventoService.calcularXpCreditado).

   Os textos abaixo são só o ponto de partida que aparece no /admin no
   primeiro boot; a partir daí quem manda é o que está no banco. Por isso
   editar um `nome` ou `descricao` aqui NÃO muda nada num banco que já
   passou pelo seed — mudar de verdade é no /admin. O `codigo`, esse sim,
   nunca deve ser alterado: é ele que liga a linha do banco à regra. */
public final class CatalogoConquistas {

    public static final String CODIGO_PRESENCA_TOTAL = "PRESENCA_TOTAL";
    public static final String CODIGO_DIA_COMPLETO = "DIA_COMPLETO";
    public static final String CODIGO_MINICURSO_CONCLUIDO = "MINICURSO_CONCLUIDO";
    public static final String CODIGO_CARTAZ_COMPLETO = "CARTAZ_COMPLETO";

    /* Pontos com que toda conquista nasce. É só um ponto de partida
       razoável para a presidência ajustar no /admin — o que segura a
       vitrine é o flag `ativa`, não a pontuação. */
    public static final int PONTOS_INICIAIS = 10;

    /* Uma conquista do catálogo. `raridade` é só o tier visual do card
       (1 comum a 5 lendária) e não entra no cálculo de pontos. */
    public record ConquistaSemeada(
            String codigo,
            String nome,
            String descricao,
            TipoValidacaoConquista tipoValidacao,
            int raridade,
            int ordem
    ) {}

    public static final List<ConquistaSemeada> CONQUISTAS = List.of(
            new ConquistaSemeada(
                    CODIGO_PRESENCA_TOTAL,
                    "Presença Total",
                    "Esteja presente em todas as palestras, mesas redondas e debates da semana, "
                            + "e em todos os minicursos que você escolheu.",
                    AUTOMATICA, 5, 1),
            new ConquistaSemeada(
                    CODIGO_DIA_COMPLETO,
                    "Dia Cheio",
                    "Esteja presente em tudo o que você tinha marcado em um mesmo dia do evento.",
                    AUTOMATICA, 2, 2),
            new ConquistaSemeada(
                    CODIGO_MINICURSO_CONCLUIDO,
                    "Minicurso Concluído",
                    "Compareça a todos os encontros de um minicurso em que você se inscreveu.",
                    AUTOMATICA, 3, 3),
            new ConquistaSemeada(
                    CODIGO_CARTAZ_COMPLETO,
                    "Cartaz Completo",
                    "Complete o cartaz da SEMAC com todos os carimbos e mostre seu QR code "
                            + "para alguém da comissão validar.",
                    MANUAL, 4, 4)
    );

    private CatalogoConquistas() {
    }
}
