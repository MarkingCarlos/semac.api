package com.semac.java_api.service;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* Transforma o Markdown editado no /admin no HTML que vai dentro do
   e-mail. Três etapas, nessa ordem:

     1. Markdown -> HTML, com HTML bruto DESLIGADO;
     2. estilo inline nas tags geradas;
     3. troca de {{variavel}} pelos valores, escapados.

   Por que HTML bruto fica desligado (escapeHtml): sem isso, escolher
   Markdown não protegeria nada — bastaria colar uma tag no editor para
   injetar o que quisesse no e-mail enviado a centenas de pessoas.

   Por que a substituição NÃO passa pelo Thymeleaf: o Thymeleaf avalia
   SpEL, então mandar texto editável pelo motor abriria injeção de
   expressão — uma conta de admin comprometida viraria execução de código
   no servidor. Troca literal de string não tem esse risco. O Thymeleaf
   continua cuidando só do envelope, que é código nosso.

   Por que estilo inline: Gmail e Outlook descartam <style>, então cada
   elemento precisa carregar o próprio `style=`. */
@Service
public class RenderizadorEmailService {

    private static final Locale LOCALE_BR = Locale.forLanguageTag("pt-BR");

    private static final Pattern VARIAVEL = Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_]*)\\s*}}");

    /* Um parágrafo que contém só um link vira botão. Regra combinada com a
       comissão e documentada na ajuda da tela: é como se escreve uma
       chamada para ação sem precisar saber HTML. */
    private static final Pattern PARAGRAFO_SO_COM_LINK =
            Pattern.compile("<p[^>]*>(<a\\s[^>]*>[^<]*</a>)</p>");

    /* CommonMark gera tags sem atributo (<p>, <li>, <blockquote>...), então
       a troca literal abaixo é segura e previsível. */
    private static final Map<String, String> ESTILOS = new LinkedHashMap<>();

    static {
        ESTILOS.put("<p>", "<p style=\"margin:0 0 16px; color:#333333; font-size:16px; line-height:1.6;\">");
        ESTILOS.put("<h1>", "<h1 style=\"margin:0 0 16px; color:#590629; font-size:24px;\">");
        ESTILOS.put("<h2>", "<h2 style=\"margin:0 0 12px; color:#590629; font-size:20px;\">");
        ESTILOS.put("<h3>", "<h3 style=\"margin:0 0 12px; color:#590629; font-size:17px;\">");
        ESTILOS.put("<ul>", "<ul style=\"margin:0 0 16px; padding-left:20px; color:#333333; font-size:16px; line-height:1.6;\">");
        ESTILOS.put("<ol>", "<ol style=\"margin:0 0 16px; padding-left:20px; color:#333333; font-size:16px; line-height:1.6;\">");
        ESTILOS.put("<li>", "<li style=\"margin:0 0 8px;\">");
        ESTILOS.put("<blockquote>", "<blockquote style=\"margin:0 0 24px; padding:16px; background-color:#faf6f2; "
                + "border-left:4px solid #E79839; color:#666666; font-size:14px; line-height:1.6;\">");
        ESTILOS.put("<hr />", "<hr style=\"border:0; border-top:1px solid #eeeeee; margin:24px 0;\" />");
        ESTILOS.put("<strong>", "<strong style=\"color:#A10535;\">");
        ESTILOS.put("<a href=", "<a style=\"color:#A10535; text-decoration:underline;\" href=");
    }

    private static final String ESTILO_BOTAO =
            "display:inline-block; background-color:#A10535; color:#ffffff; text-decoration:none; "
                    + "padding:14px 28px; border-radius:6px; font-size:16px; font-weight:bold;";

    private final Parser parser = Parser.builder().build();

    /* escapeHtml(true) é a linha que torna o Markdown uma escolha de
       segurança, e não só de conforto. */
    private final HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).build();

    private final TemplateEngine motorTemplates;

    public RenderizadorEmailService(TemplateEngine motorTemplates) {
        this.motorTemplates = motorTemplates;
    }

    /* Corpo Markdown + variáveis -> e-mail completo, pronto para enviar. */
    public String renderizar(String corpoMarkdown, Map<String, String> variaveis) {
        String corpoHtml = aplicarEstilos(renderer.render(parser.parse(corpoMarkdown)));
        corpoHtml = substituirVariaveis(corpoHtml, variaveis);

        Context contexto = new Context(LOCALE_BR);
        contexto.setVariable("corpoHtml", corpoHtml);
        return motorTemplates.process("email/envelope", contexto);
    }

    private String aplicarEstilos(String html) {
        String resultado = html;
        for (Map.Entry<String, String> estilo : ESTILOS.entrySet()) {
            resultado = resultado.replace(estilo.getKey(), estilo.getValue());
        }
        return transformarLinksSolitariosEmBotao(resultado);
    }

    private String transformarLinksSolitariosEmBotao(String html) {
        Matcher matcher = PARAGRAFO_SO_COM_LINK.matcher(html);
        StringBuilder saida = new StringBuilder();
        while (matcher.find()) {
            String link = matcher.group(1)
                    .replaceFirst("style=\"[^\"]*\"", Matcher.quoteReplacement("style=\"" + ESTILO_BOTAO + "\""));
            matcher.appendReplacement(saida, Matcher.quoteReplacement(
                    "<p style=\"margin:0 0 24px; text-align:center;\">" + link + "</p>"));
        }
        matcher.appendTail(saida);
        return saida.toString();
    }

    /* Variável sem valor vira string vazia em vez de aparecer como
       {{algo}} no e-mail de centenas de pessoas. O que impede erro de
       digitação de chegar tão longe é a validação na hora de salvar
       (ModeloEmailService.validarVariaveis). */
    private String substituirVariaveis(String html, Map<String, String> variaveis) {
        Matcher matcher = VARIAVEL.matcher(html);
        StringBuilder saida = new StringBuilder();
        while (matcher.find()) {
            String valor = variaveis.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(saida, Matcher.quoteReplacement(escaparHtml(valor)));
        }
        matcher.appendTail(saida);
        return saida.toString();
    }

    /* Escapa o valor, não o template: um participante chamado
       "Ana <script>" não pode virar tag no e-mail. Serve tanto para texto
       quanto para href, que é onde as variáveis de URL caem. */
    private String escaparHtml(String valor) {
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /* Erro de digitação em {{nomeParticipnte}} não pode virar e-mail em
       branco para centenas de pessoas: barra antes de salvar ou disparar.
       Vive aqui porque esta classe é a dona da sintaxe {{...}}, e tanto as
       mensagens automáticas quanto os comunicados avulsos usam a regra. */
    public void validarVariaveis(String corpoMarkdown, java.util.Set<String> permitidas) {
        java.util.List<String> desconhecidas = variaveisUsadas(corpoMarkdown).stream()
                .filter(v -> !permitidas.contains(v))
                .toList();

        if (!desconhecidas.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Variável não existe nesta mensagem: " + String.join(", ", desconhecidas)
                            + ". Disponíveis: " + String.join(", ", permitidas) + ".");
        }
    }

    /* Nomes de variável usados num corpo — base da validação acima. */
    public java.util.Set<String> variaveisUsadas(String corpoMarkdown) {
        java.util.Set<String> usadas = new java.util.LinkedHashSet<>();
        Matcher matcher = VARIAVEL.matcher(corpoMarkdown);
        while (matcher.find()) {
            usadas.add(matcher.group(1));
        }
        return usadas;
    }
}
