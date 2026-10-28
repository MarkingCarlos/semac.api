package com.semac.java_api.repository.projection;

/* Quantas pessoas responderam e quantas gabaritaram o quiz de cada
   laboratório — os contadores do /admin. */
public interface ContagemRespostasLaboratorioView {
    Integer getLaboratorioId();
    Long getRespostas();
    Long getGabaritos();
}
