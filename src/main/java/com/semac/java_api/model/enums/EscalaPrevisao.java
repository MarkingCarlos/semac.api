package com.semac.java_api.model.enums;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/* Escalas por cabeça que multiplicam o valor de um item previsto.

   Um item pode ter várias, e o fator é a SOMA dos contadores (ex.:
   crachá para inscritos + comissão). Item sem nenhuma é valor fechado
   (fator 1) — por isso não existe mais um valor FIXA.

   POR_INSCRITO conta só quem ganha kit (sem ingresso diário);
   POR_INSCRITO_TOTAL conta também o diário. As duas nunca vão juntas:
   contariam as mesmas pessoas duas vezes. */
public enum EscalaPrevisao {
    POR_INSCRITO,
    POR_INSCRITO_TOTAL,
    POR_COMISSAO,
    POR_PALESTRANTE;

    public static EscalaPrevisao deTexto(String texto) {
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escala inválida: " + texto);
        }
    }

    /* Lista vinda do cliente → conjunto validado. Null ou vazia = valor
       fechado. */
    public static Set<EscalaPrevisao> deTextos(List<String> textos) {
        Set<EscalaPrevisao> escalas = EnumSet.noneOf(EscalaPrevisao.class);
        if (textos == null) return escalas;
        for (String texto : textos) escalas.add(deTexto(texto));
        if (escalas.contains(POR_INSCRITO) && escalas.contains(POR_INSCRITO_TOTAL)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Escolha só uma escala de inscritos: com kit ou incluindo diária.");
        }
        return escalas;
    }
}
