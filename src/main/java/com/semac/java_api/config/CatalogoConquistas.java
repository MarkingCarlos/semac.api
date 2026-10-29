package com.semac.java_api.config;

import com.semac.java_api.model.enums.TipoValidacaoConquista;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public static final String CODIGO_ATIVIDADES_NOTURNAS = "ATIVIDADES_NOTURNAS";
    public static final String CODIGO_ABERTURA_PRESENTE = "ABERTURA_PRESENTE";
    public static final String CODIGO_COFFEE_BREAK_COMPLETO = "COFFEE_BREAK_COMPLETO";
    public static final String CODIGO_DEBATES_COMPLETO = "DEBATES_COMPLETO";
    public static final String CODIGO_MOSTRA_COMPLETA = "MOSTRA_COMPLETA";
    public static final String CODIGO_PERGUNTA_DEBATE = "PERGUNTA_DEBATE";
    public static final String CODIGO_AVALIACAO_COMPLETA = "AVALIACAO_COMPLETA";
    public static final String CODIGO_PERGUNTA_ESCOLHIDA = "PERGUNTA_ESCOLHIDA";
    public static final String CODIGO_LIGEIRINHO = "LIGEIRINHO";
    public static final String CODIGO_ALQUIMIA_COMPLETA = "ALQUIMIA_COMPLETA";
    public static final String CODIGO_REI_CAMALEAO = "REI_CAMALEAO";
    public static final String CODIGO_DETETIVE_PICAKU = "DETETIVE_PICAKU";

    /* Vagas no pódio da tarefa de cada minicurso (ALQUIMIA_COMPLETA). */
    public static final int VAGAS_PODIO_TAREFA_MINICURSO = 3;

    /* Conquistas manuais que MEMBRO também concede — o pódio do minicurso
       é validado por quem está na sala. As outras seguem só com diretoria
       e presidência (ver ConquistaService.exigirPapelDeConcessao). */
    public static final Set<String> CONQUISTAS_CONCEDIDAS_POR_MEMBRO = Set.of(CODIGO_ALQUIMIA_COMPLETA);

    /* Último dia (inclusive) em que a comissão pode conceder uma conquista
       MANUAL. Conquista fora do mapa não tem prazo. Diretoria de site e
       presidência concedem a qualquer momento (ver
       ConquistaService.exigirPrazoDeConcessao). */
    public static final Map<String, LocalDate> PRAZOS_CONCESSAO_MANUAL = Map.of(
            CODIGO_PERGUNTA_ESCOLHIDA, LocalDate.of(2026, 10, 26));

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
                    "Esteja presente na abertura, no encerramento e em todas as palestras, "
                            + "mesas redondas, debates, mostras técnicas e atividades noturnas da "
                            + "semana, e em todos os minicursos que você escolheu.",
                    AUTOMATICA, 5, 1),
            new ConquistaSemeada(
                    CODIGO_DIA_COMPLETO,
                    "Dia Cheio",
                    "Esteja presente em tudo o que você tinha marcado em um mesmo dia do evento.",
                    AUTOMATICA, 2, 2),
            new ConquistaSemeada(
                    CODIGO_MINICURSO_CONCLUIDO,
                    "Minicurso Concluído",
                    "Compareça a todos os minicurso em que você se inscreveu.",
                    AUTOMATICA, 3, 3),
            new ConquistaSemeada(
                    CODIGO_CARTAZ_COMPLETO,
                    "Cartaz Completo",
                    "Complete o cartaz da SEMAC com todos os carimbos e mostre seu QR code "
                            + "para alguém da comissão validar.",
                    MANUAL, 4, 4),
            new ConquistaSemeada(
                    CODIGO_ATIVIDADES_NOTURNAS,
                    "Criatura das Trevas",
                    "Esteja presente na Game Night da SEMAC.",
                    AUTOMATICA, 2, 5),
            new ConquistaSemeada(
                    CODIGO_ABERTURA_PRESENTE,
                    "E lá vamos nós",
                    "Esteja presente na abertura da SEMAC.",
                    AUTOMATICA, 1, 6),
            new ConquistaSemeada(
                    CODIGO_COFFEE_BREAK_COMPLETO,
                    "Café com Java",
                    "Esteja presente em todos os coffee breaks da SEMAC.",
                    AUTOMATICA, 1, 7),
            new ConquistaSemeada(
                    CODIGO_DEBATES_COMPLETO,
                    "Treta Saudável",
                    "Esteja presente em todos os debates da SEMAC.",
                    AUTOMATICA, 1, 8),
            new ConquistaSemeada(
                    CODIGO_MOSTRA_COMPLETA,
                    "Explorador da Mostra",
                    "Visite todos os trabalhos da Mostra Técnica lendo o QR code de cada stand.",
                    AUTOMATICA, 3, 9),
            new ConquistaSemeada(
                    CODIGO_PERGUNTA_DEBATE,
                    "Pergunta de Ouro",
                    "Envie uma pergunta para um dos debates da SEMAC.",
                    AUTOMATICA, 1, 10),
            new ConquistaSemeada(
                    CODIGO_AVALIACAO_COMPLETA,
                    "Voz da Galera",
                    "Quando a semana acabar, deixe sua nota em todas as atividades em que você "
                            + "esteve presente.",
                    AUTOMATICA, 2, 11),
            new ConquistaSemeada(
                    CODIGO_PERGUNTA_ESCOLHIDA,
                    "O Escolhido",
                    "Tenha sua pergunta escolhida pela mediação de um dos debates da SEMAC.",
                    MANUAL, 3, 12),
            new ConquistaSemeada(
                    CODIGO_LIGEIRINHO,
                    "Ligeirinho",
                    "Seja o primeiro participante do dia a vencer um desafio da SEMAC — "
                            + "o Termo ou a Cifra do Dia, o que sair primeiro.",
                    AUTOMATICA, 4, 13),
            new ConquistaSemeada(
                    CODIGO_ALQUIMIA_COMPLETA,
                    "Alquimia Completa",
                    "Esteja entre os 3 primeiros a concluir a tarefa de um minicurso.",
                    MANUAL, 3, 14),
            new ConquistaSemeada(
                    CODIGO_REI_CAMALEAO,
                    "Rei Camaleão",
                    "Não teve seu boneco encontrado na Oficina de Impressão 3D.",
                    MANUAL, 3, 15),
            new ConquistaSemeada(
                    CODIGO_DETETIVE_PICAKU,
                    "Detetive Picaku",
                    "Encontrou mais bonecos na Oficina de Impressão 3D.",
                    MANUAL, 3, 16)
    );

    private CatalogoConquistas() {
    }
}
