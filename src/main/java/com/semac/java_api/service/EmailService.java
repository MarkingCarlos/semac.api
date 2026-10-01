package com.semac.java_api.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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

    /* Logo do cabeçalho do envelope (templates/email/envelope.html), que o
       referencia como cid:logoSemac. Vai anexado inline em todo e-mail em
       vez de ser um link para o site: imagem externa é bloqueada por
       padrão em boa parte dos clientes. */
    private static final String CID_LOGO_EMAIL = "logoSemac";
    private static final ClassPathResource IMAGEM_LOGO_EMAIL = new ClassPathResource("email/logoSemac2026.png");

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

    /* Prévia do /admin: o HTML vai para um <iframe srcDoc>, onde não existe
       anexo inline e o navegador não resolve cid: (ERR_UNKNOWN_URL_SCHEME).
       Troca a referência pelo próprio PNG em data URI. Só para exibir —
       o que é enviado de verdade continua usando cid:. */
    public String htmlParaPreviaNoNavegador(String html) {
        if (html == null) {
            return null;
        }
        String dataUriLogo = dataUriLogoEmail();
        if (dataUriLogo == null) {
            return html;
        }
        return html.replace("cid:" + CID_LOGO_EMAIL, dataUriLogo);
    }

    /* Calculado uma vez: o arquivo vem do classpath e não muda em execução. */
    private volatile String dataUriLogoEmailEmCache;

    private String dataUriLogoEmail() {
        if (dataUriLogoEmailEmCache == null && IMAGEM_LOGO_EMAIL.exists()) {
            try (InputStream entrada = IMAGEM_LOGO_EMAIL.getInputStream()) {
                dataUriLogoEmailEmCache = "data:image/png;base64,"
                        + Base64.getEncoder().encodeToString(entrada.readAllBytes());
            } catch (IOException e) {
                log.warn("Não foi possível ler o logo do e-mail para a prévia: {}", e.getMessage());
            }
        }
        return dataUriLogoEmailEmCache;
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
            /* multipart = true: sem ele não dá para anexar o logo inline. */
            MimeMessageHelper montador =
                    new MimeMessageHelper(mensagem, true, StandardCharsets.UTF_8.name());
            montador.setFrom(emailRemetente, nomeRemetente);
            montador.setTo(destinatario);
            montador.setSubject(assunto);
            // setText antes de addInline: o JavaMail exige o corpo antes dos anexos inline.
            montador.setText(html, true);
            /* Sem o arquivo, o e-mail sai mesmo assim — o cliente mostra o
               alt do <img>. Um logo faltando não pode barrar confirmação. */
            if (IMAGEM_LOGO_EMAIL.exists()) {
                montador.addInline(CID_LOGO_EMAIL, IMAGEM_LOGO_EMAIL, "image/png");
            } else {
                log.warn("Logo do e-mail não encontrado no classpath ({}) — enviando sem ele.",
                        IMAGEM_LOGO_EMAIL.getPath());
            }

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
