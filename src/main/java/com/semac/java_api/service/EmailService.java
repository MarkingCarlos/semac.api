package com.semac.java_api.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/* Envio de e-mail transacional da SEMAC. Só cuida do "como enviar" —
   quem decide o que mandar e para quem são os listeners de notificação.

   Duas regras que valem para todo envio daqui:
   - é assíncrono: SMTP do Gmail leva segundos, e nenhum participante pode
     ficar esperando isso no meio de um request HTTP;
   - nunca propaga exceção: falha de envio vira log de erro. Confirmação de
     inscrição não pode ser desfeita porque o servidor de e-mail caiu. */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender remetenteSmtp;
    private final boolean habilitado;
    private final String emailRemetente;
    private final String nomeRemetente;

    public EmailService(JavaMailSender remetenteSmtp,
                        @Value("${app.mail.habilitado}") boolean habilitado,
                        @Value("${app.mail.remetente}") String emailRemetente,
                        @Value("${app.mail.nome}") String nomeRemetente) {
        this.remetenteSmtp = remetenteSmtp;
        this.habilitado = habilitado;
        this.emailRemetente = emailRemetente;
        this.nomeRemetente = nomeRemetente;
    }

    /* Envia um HTML ja montado, em segundo plano. Quem monta e o
       RenderizadorEmailService, a partir do corpo Markdown guardado no
       banco — este serviço nao sabe nada sobre o conteudo, so sobre como
       entregar. */
    @Async("executorEmail")
    public void enviarHtmlPronto(String destinatario, String assunto, String html) {
        enviarAgora(destinatario, assunto, html);
    }

    /* Três resultados, não dois: "não enviei porque está desligado" não é
       falha. Sem essa distinção, um lote rodado com app.mail.habilitado=false
       apareceria no histórico como "concluído com falhas", reportando um
       problema que não existe. */
    public enum ResultadoEnvio {
        ENVIADO,
        /* Envio desligado ou destinatário sem e-mail — nada a fazer. */
        IGNORADO,
        FALHOU
    }

    /* Versão síncrona, que devolve o resultado. Existe para o disparo em
       lote (ComunicadoService): enfileirar 400 envios assíncronos
       devolveria o controle na hora e não haveria como contar quantos
       falharam nem quando o lote terminou. */
    public ResultadoEnvio enviarAgora(String destinatario, String assunto, String html) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("E-mail \"{}\" não enviado: destinatário vazio.", assunto);
            return ResultadoEnvio.IGNORADO;
        }
        if (html == null) {
            log.info("E-mail \"{}\" não enviado para {}: mensagem desligada em /admin -> Mensagens.",
                    assunto, destinatario);
            return ResultadoEnvio.IGNORADO;
        }
        if (!habilitado) {
            log.info("Envio desabilitado (app.mail.habilitado=false) — \"{}\" para {} não foi enviado.",
                    assunto, destinatario);
            return ResultadoEnvio.IGNORADO;
        }

        try {
            MimeMessage mensagem = remetenteSmtp.createMimeMessage();
            MimeMessageHelper montador =
                    new MimeMessageHelper(mensagem, false, StandardCharsets.UTF_8.name());
            montador.setFrom(emailRemetente, nomeRemetente);
            montador.setTo(destinatario);
            montador.setSubject(assunto);
            montador.setText(html, true);

            remetenteSmtp.send(mensagem);
            log.info("E-mail \"{}\" enviado para {}.", assunto, destinatario);
            return ResultadoEnvio.ENVIADO;
        } catch (Exception e) {
            /* Exception geral de propósito: erro de SMTP, de encoding ou de
               montagem — nenhum deles pode escapar e derrubar o fluxo que
               pediu o envio. */
            log.error("Falha ao enviar e-mail \"{}\" para {}: {}", assunto, destinatario, e.getMessage(), e);
            return ResultadoEnvio.FALHOU;
        }
    }
}
