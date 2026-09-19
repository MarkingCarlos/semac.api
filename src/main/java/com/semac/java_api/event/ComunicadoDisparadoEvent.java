package com.semac.java_api.event;

import java.util.List;

/* Disparado quando um comunicado avulso é registrado. O envio em si só
   começa depois do commit: se a transação que gravou o comunicado der
   rollback, nenhuma mensagem pode ter saído.

   Os destinatários vêm como valores já extraídos, não entidades: o envio
   roda fora da transação e em outra thread, onde acesso lazy estouraria. */
public record ComunicadoDisparadoEvent(
        Integer comunicadoId,
        String assunto,
        String corpoMarkdown,
        List<Destinatario> destinatarios
) {
    public record Destinatario(String nome, String email) {
    }
}
