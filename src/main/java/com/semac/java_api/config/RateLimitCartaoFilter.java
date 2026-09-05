package com.semac.java_api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/* Freia a velocidade de chamadas a POST /api/pagamento/cartao por IP —
   complementa o limite por inscrição em PagamentoCartaoService, que sozinho
   não impede alguém de contornar o limite criando várias inscrições
   descartáveis (POST /api/inscricao, também público) e testando um cartão
   em cada uma. Janela fixa em memória: sem dependência nova (Bucket4j seria
   overkill pra um único endpoint) e sem limpeza periódica — o volume de
   IPs distintos que chamam esse endpoint é pequeno pro tamanho do evento.
   Só funciona corretamente atrás do proxy reverso se
   server.forward-headers-strategy=framework estiver configurado (ver
   application.properties), senão todo mundo cai no mesmo IP. */
public class RateLimitCartaoFilter extends OncePerRequestFilter {

    private static final int LIMITE_REQUISICOES = 10;
    private static final long JANELA_MILLIS = 15 * 60 * 1000L;

    private final Clock relogio;
    private final ConcurrentHashMap<String, ContadorJanela> contadoresPorIp = new ConcurrentHashMap<>();

    public RateLimitCartaoFilter() {
        this(Clock.systemUTC());
    }

    RateLimitCartaoFilter(Clock relogio) {
        this.relogio = relogio;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        ContadorJanela contador = contadoresPorIp.computeIfAbsent(ip, k -> new ContadorJanela());

        if (!contador.registrarTentativa(relogio.millis())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"mensagem\":\"Muitas tentativas de pagamento. Aguarde alguns minutos e tente novamente.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    /* Contagem de uma janela fixa de JANELA_MILLIS: passado esse tempo desde
       o início, a janela reseta sozinha na próxima requisição. */
    private static final class ContadorJanela {
        private volatile long inicioJanela = 0;
        private final AtomicInteger contagem = new AtomicInteger(0);

        synchronized boolean registrarTentativa(long agora) {
            if (agora - inicioJanela >= JANELA_MILLIS) {
                inicioJanela = agora;
                contagem.set(0);
            }
            return contagem.incrementAndGet() <= LIMITE_REQUISICOES;
        }
    }
}
