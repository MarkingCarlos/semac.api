package com.semac.java_api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/* Freia, por IP, os POST públicos que servem de alvo para força bruta ou
   abuso: login, verificação do código de ingresso, cadastro e envio de
   comprovante (ver RateLimitConfig). Mesma janela fixa em memória do
   RateLimitCartaoFilter, só que com limite, janela e mensagem por rota.

   Conta apenas POST: os padrões de URL de servlet não distinguem método,
   e GET/PUT nas mesmas rotas (ex.: /api/tipo-inscricao/{id}) não devem
   gastar a cota. Mesma ressalva do proxy reverso do filtro do cartão:
   depende de server.forward-headers-strategy=framework. */
public class RateLimitPorIpFilter extends OncePerRequestFilter {

    private final int limiteRequisicoes;
    private final long janelaMillis;
    private final String corpoRespostaBloqueio;
    private final ConcurrentHashMap<String, ContadorJanela> contadoresPorIp = new ConcurrentHashMap<>();

    public RateLimitPorIpFilter(int limiteRequisicoes, Duration janela, String mensagemBloqueio) {
        this.limiteRequisicoes = limiteRequisicoes;
        this.janelaMillis = janela.toMillis();
        this.corpoRespostaBloqueio = "{\"mensagem\":\"" + mensagemBloqueio + "\"}";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        ContadorJanela contador = contadoresPorIp.computeIfAbsent(request.getRemoteAddr(), k -> new ContadorJanela());

        if (!contador.registrarTentativa(System.currentTimeMillis())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(corpoRespostaBloqueio);
            return;
        }

        chain.doFilter(request, response);
    }

    private final class ContadorJanela {
        private long inicioJanela = 0;
        private int contagem = 0;

        synchronized boolean registrarTentativa(long agora) {
            if (agora - inicioJanela >= janelaMillis) {
                inicioJanela = agora;
                contagem = 0;
            }
            return ++contagem <= limiteRequisicoes;
        }
    }
}
