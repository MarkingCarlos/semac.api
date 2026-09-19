package com.semac.java_api;

import com.semac.java_api.service.RenderizadorEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/* Cobre o que o corpo editável no /admin pode fazer com o e-mail — em
   especial os dois pontos de segurança: HTML bruto vindo do editor e
   valores de variável vindos do banco. */
class RenderizadorEmailTest {

    private RenderizadorEmailService renderizador;

    @BeforeEach
    void preparar() {
        ClassLoaderTemplateResolver resolvedor = new ClassLoaderTemplateResolver();
        resolvedor.setPrefix("templates/");
        resolvedor.setSuffix(".html");
        resolvedor.setTemplateMode(TemplateMode.HTML);
        resolvedor.setCharacterEncoding("UTF-8");

        SpringTemplateEngine motor = new SpringTemplateEngine();
        motor.setTemplateResolver(resolvedor);

        renderizador = new RenderizadorEmailService(motor);
    }

    @Test
    void substituiVariaveisEMantemOEnvelope() {
        String html = renderizador.renderizar(
                "Olá, **{{nomeParticipante}}**!",
                Map.of("nomeParticipante", "Maria Souza"));

        assertTrue(html.contains("Maria Souza"));
        assertTrue(html.contains("SEMAC XXXVI"), "o envelope deve envolver o corpo");
        assertTrue(html.contains("semacsjrp@gmail.com"), "o rodapé deve continuar presente");
        assertFalse(html.contains("{{"), "nenhum placeholder pode sobrar");
    }

    /* A razão de ter escolhido Markdown em vez de HTML puro no editor. */
    @Test
    void escapaHtmlBrutoEscritoNoEditor() {
        String html = renderizador.renderizar(
                "Texto <script>alert('xss')</script> e <img src=x onerror=alert(1)>",
                Map.of());

        /* O texto "onerror=alert(1)" continua no HTML, mas como conteúdo
           escapado dentro de &lt;img ...&gt; — o que importa é que nenhuma
           TAG real foi criada. Por isso a checagem é pelo "<img", não pela
           substring do atributo. */
        assertFalse(html.contains("<script>"), "tag vinda do editor não pode virar HTML");
        assertFalse(html.contains("<img"), "tag vinda do editor não pode virar HTML");
        assertTrue(html.contains("&lt;script&gt;"), "deve aparecer como texto escapado");
        assertTrue(html.contains("&lt;img"), "deve aparecer como texto escapado");
    }

    /* Um participante chamado "Ana <script>" não pode quebrar o e-mail. */
    @Test
    void escapaValorDeVariavel() {
        String html = renderizador.renderizar(
                "Olá, {{nomeParticipante}}!",
                Map.of("nomeParticipante", "Ana <script>alert(1)</script>"));

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    /* Detalhe real do CommonMark: destino de link pode sair percent-encoded,
       o que transformaria {{url}} em %7B%7Burl%7D%7D e mataria o link. */
    @Test
    void naoPercentEncodaVariavelNoDestinoDoLink() {
        String html = renderizador.renderizar(
                "[Acessar área do participante]({{urlAreaParticipante}})",
                Map.of("urlAreaParticipante", "https://semac.cc/participantes"));

        assertFalse(html.contains("%7B"), "o placeholder não pode ser percent-encoded antes da troca");
        assertTrue(html.contains("href=\"https://semac.cc/participantes\""));
    }

    @Test
    void linkSozinhoNoParagrafoViraBotao() {
        String html = renderizador.renderizar(
                "[Acessar área do participante]({{url}})",
                Map.of("url", "https://semac.cc/participantes"));

        assertTrue(html.contains("background-color:#A10535"), "link solitário deve virar botão");
        assertTrue(html.contains("text-align:center"));
    }

    @Test
    void citacaoViraCaixaDestacada() {
        String html = renderizador.renderizar("> Guarde este e-mail.", Map.of());

        assertTrue(html.contains("border-left:4px solid #E79839"));
        assertTrue(html.contains("Guarde este e-mail."));
    }

    /* Estilo inline é obrigatório: Gmail descarta <style>. */
    @Test
    void aplicaEstiloInlineNasTagsGeradas() {
        String html = renderizador.renderizar("Um parágrafo.\n\n- item", Map.of());

        assertTrue(html.contains("<p style=\"margin:0 0 16px;"));
        assertTrue(html.contains("<li style="));
    }

    @Test
    void listaVariaveisUsadas() {
        var usadas = renderizador.variaveisUsadas("{{nomeParticipante}} e {{ valorIngresso }} e {{nomeParticipante}}");

        assertEquals(2, usadas.size());
        assertTrue(usadas.contains("nomeParticipante"));
        assertTrue(usadas.contains("valorIngresso"), "espaços dentro das chaves devem ser tolerados");
    }

    /* Variável sem valor some, em vez de sair {{algo}} para o participante. */
    @Test
    void variavelSemValorViraVazio() {
        String html = renderizador.renderizar("Diárias: {{diasInscricao}}", Map.of());

        assertFalse(html.contains("{{diasInscricao}}"));
        assertTrue(html.contains("Diárias:"));
    }
}
