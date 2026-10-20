package com.semac.java_api.repository.projection;

/* Quantas visitas cada trabalho da Mostra Técnica já recebeu — o contador
   ao lado de cada trabalho no /admin. Trabalho sem visita não aparece. */
public interface ContagemVisitasTrabalhoMostraView {
    Integer getTrabalhoId();
    Long getTotal();
}
