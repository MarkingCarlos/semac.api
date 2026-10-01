package com.semac.java_api.config;

import com.semac.java_api.model.enums.CodigoTipoEvento;

import java.util.List;

import static com.semac.java_api.model.enums.CodigoTipoEvento.*;

/* Todo tipo de evento que existe no sistema está declarado aqui — mesmo
   esquema de CatalogoConquistas. O TipoEventoSeedRunner garante que cada
   entrada tenha sua linha em `tipo_evento`.

   Nome, pontos e exigeInscricao abaixo são só o ponto de partida de um
   banco novo: depois da primeira inserção quem manda é o /admin (aba
   Conteúdo e regras de xp). Os valores espelham o que estava em produção
   quando a V49 introduziu o código. */
public final class CatalogoTiposEvento {

    public record TipoEventoSemeado(
            CodigoTipoEvento codigo,
            String nome,
            int pontos,
            boolean exigeInscricao
    ) {}

    public static final List<TipoEventoSemeado> TIPOS = List.of(
            new TipoEventoSemeado(ABERTURA, "Abertura", 0, false),
            new TipoEventoSemeado(ATIVIDADES_NOTURNAS, "Atividades Noturnas", 15, false),
            new TipoEventoSemeado(COFFEE_BREAK, "Coffee-Break", 0, false),
            new TipoEventoSemeado(CREDENCIAMENTO, "Credenciamento", 0, false),
            new TipoEventoSemeado(DEBATE, "Debate", 20, false),
            new TipoEventoSemeado(ENCERRAMENTO, "Encerramento", 0, false),
            new TipoEventoSemeado(MESA_REDONDA, "Mesa Redonda", 20, false),
            new TipoEventoSemeado(MINICURSO, "Minicurso", 35, true),
            new TipoEventoSemeado(MOSTRA_TECNICA, "Mostra Técnica", 35, false),
            new TipoEventoSemeado(PALESTRA, "Palestra", 20, false)
    );

    private CatalogoTiposEvento() {
    }
}
