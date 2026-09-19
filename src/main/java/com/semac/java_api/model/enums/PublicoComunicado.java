package com.semac.java_api.model.enums;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/* Para quem um comunicado avulso pode ser disparado.

   Deliberadamente uma lista fechada, e não um filtro livre: e-mail em
   massa não tem desfazer, e escolher errado num campo aberto custa caro.
   Toda opção aqui exclui pessoas desativadas e sem e-mail (ver
   ComunicadoService.resolverDestinatarios). */
public enum PublicoComunicado {

    /* role = PARTICIPANTE: quem teve a inscrição confirmada. */
    PARTICIPANTES_CONFIRMADOS("Participantes confirmados", false),

    /* role = NULL: inscreveu-se e ainda aguarda confirmação. Útil para
       cobrar comprovante de pagamento. */
    INSCRICOES_PENDENTES("Inscrições pendentes", false),

    /* Qualquer papel de comissão (MEMBRO, DIRETOR_*, PRESIDENTE). */
    COMISSAO("Comissão organizadora", false),

    /* Quem está inscrito num evento específico — o caso do "a sala do
       minicurso mudou". */
    INSCRITOS_EM_EVENTO("Inscritos em um evento", true);

    private final String rotulo;
    private final boolean exigeEvento;

    PublicoComunicado(String rotulo, boolean exigeEvento) {
        this.rotulo = rotulo;
        this.exigeEvento = exigeEvento;
    }

    public String getRotulo() {
        return rotulo;
    }

    public boolean isExigeEvento() {
        return exigeEvento;
    }

    public static PublicoComunicado deTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha o público do comunicado.");
        }
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Público inválido: " + texto);
        }
    }
}
