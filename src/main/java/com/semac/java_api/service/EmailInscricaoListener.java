package com.semac.java_api.service;

import com.semac.java_api.config.CatalogoVariaveisEmail;
import com.semac.java_api.event.InscricaoConfirmadaEvent;
import com.semac.java_api.service.ModeloEmailService.MensagemPronta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/* Traduz eventos de inscrição em e-mail para o participante.

   Este listener só monta os VALORES das variáveis. O texto em volta deles
   vive em `modelo_email` e é editado em /admin -> Mensagens, sem deploy.

   AFTER_COMMIT de propósito: se a transação da confirmação der rollback
   depois, o e-mail não pode ter saído dizendo que deu tudo certo. O
   listener em si é síncrono e rápido — quem é assíncrono é o
   EmailService.enviarHtmlPronto que ele chama. */
@Component
public class EmailInscricaoListener {

    private static final NumberFormat FORMATO_REAL =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

    private final ModeloEmailService modeloEmailService;
    private final EmailService emailService;
    private final String urlSite;

    public EmailInscricaoListener(ModeloEmailService modeloEmailService,
                                  EmailService emailService,
                                  @Value("${app.site.url}") String urlSite) {
        this.modeloEmailService = modeloEmailService;
        this.emailService = emailService;
        this.urlSite = urlSite;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoConfirmarInscricao(InscricaoConfirmadaEvent evento) {
        Map<String, String> variaveis = new LinkedHashMap<>();
        variaveis.put("nomeParticipante", texto(evento.nomeParticipante()));
        variaveis.put("nomeIngresso", texto(evento.nomeIngresso()));
        variaveis.put("valorIngresso",
                evento.valorIngresso() == null ? "" : FORMATO_REAL.format(evento.valorIngresso()));
        variaveis.put("diasInscricao",
                evento.diasInscricao() == null ? "" : String.valueOf(evento.diasInscricao()));
        variaveis.put("urlAreaParticipante", urlSite + "/participantes");

        MensagemPronta mensagem =
                modeloEmailService.montar(CatalogoVariaveisEmail.INSCRICAO_CONFIRMADA, variaveis);

        emailService.enviarHtmlPronto(evento.emailParticipante(), mensagem.assunto(), mensagem.html());
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
