package com.semac.java_api.model.enums;

/* Andamento de um disparo. CONCLUIDO_COM_FALHAS é separado de CONCLUIDO
   de propósito: um lote em que parte das mensagens não saiu precisa saltar
   aos olhos no histórico, não se esconder atrás de um "pronto". */
public enum StatusComunicado {
    EM_ANDAMENTO,
    CONCLUIDO,
    CONCLUIDO_COM_FALHAS
}
