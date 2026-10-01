package com.semac.java_api.model.enums;

/* Os tipos de evento que existem na SEMAC. Cada valor é a chave estável de
   uma linha de `tipo_evento` (coluna `codigo`, desde a V49) — é por ela,
   e nunca pelo nome, que as regras reconhecem um tipo. O nome é editável
   no /admin; o código não.

   Tipo novo = um valor aqui + uma entrada em CatalogoTiposEvento. O
   /admin não cria nem exclui tipos. */
public enum CodigoTipoEvento {
    ABERTURA,
    ATIVIDADES_NOTURNAS,
    COFFEE_BREAK,
    CREDENCIAMENTO,
    DEBATE,
    ENCERRAMENTO,
    MESA_REDONDA,
    MINICURSO,
    MOSTRA_TECNICA,
    PALESTRA
}
