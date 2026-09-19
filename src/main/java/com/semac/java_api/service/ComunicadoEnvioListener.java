package com.semac.java_api.service;

import com.semac.java_api.event.ComunicadoDisparadoEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/* Começa o envio em lote só depois do commit: se a transação que gravou o
   comunicado der rollback, nenhuma mensagem pode ter saído.

   Bean separado de propósito — chamar processarLote a partir do próprio
   ComunicadoService seria auto-invocação e o @Async não valeria, fazendo
   o lote inteiro rodar na thread de quem clicou. */
@Component
public class ComunicadoEnvioListener {

    private final ComunicadoService comunicadoService;

    public ComunicadoEnvioListener(ComunicadoService comunicadoService) {
        this.comunicadoService = comunicadoService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoDispararComunicado(ComunicadoDisparadoEvent evento) {
        comunicadoService.processarLote(evento);
    }
}
