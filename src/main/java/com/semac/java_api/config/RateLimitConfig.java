package com.semac.java_api.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import java.time.Duration;

/* Liga os limites por IP só nas rotas públicas que precisam deles — não
   afeta o resto da API. Cada rota tem o próprio filtro (e a própria
   contagem): estourar o cadastro não bloqueia o login de ninguém. */
@Configuration
public class RateLimitConfig {

    @Bean
    public FilterRegistrationBean<RateLimitCartaoFilter> rateLimitCartaoFilter() {
        FilterRegistrationBean<RateLimitCartaoFilter> registro = new FilterRegistrationBean<>();
        registro.setFilter(new RateLimitCartaoFilter());
        registro.addUrlPatterns("/api/pagamento/cartao");
        return registro;
    }

    /* Folgado de propósito: no dia do evento muita gente entra pela mesma
       rede da universidade (mesmo IP). A proteção contra força bruta numa
       conta específica é o bloqueio por e-mail do AuthController. */
    @Bean
    public FilterRegistrationBean<RateLimitPorIpFilter> rateLimitLoginFilter() {
        return registrar("rateLimitLogin", "/api/auth/login",
                new RateLimitPorIpFilter(60, Duration.ofMinutes(5),
                        "Muitas tentativas de login. Aguarde alguns minutos e tente novamente."));
    }

    /* Só POST conta, e o único POST em /api/tipo-inscricao/{id} é o
       verificar-codigo — sem isso dava pra descobrir o código de um
       ingresso restrito testando combinações. */
    @Bean
    public FilterRegistrationBean<RateLimitPorIpFilter> rateLimitCodigoIngressoFilter() {
        return registrar("rateLimitCodigoIngresso", "/api/tipo-inscricao/*",
                new RateLimitPorIpFilter(10, Duration.ofMinutes(15),
                        "Muitas tentativas de código. Aguarde alguns minutos e tente novamente."));
    }

    @Bean
    public FilterRegistrationBean<RateLimitPorIpFilter> rateLimitCadastroFilter() {
        return registrar("rateLimitCadastro", "/api/inscricao",
                new RateLimitPorIpFilter(20, Duration.ofHours(1),
                        "Muitos cadastros a partir desta rede. Aguarde um pouco e tente novamente."));
    }

    @Bean
    public FilterRegistrationBean<RateLimitPorIpFilter> rateLimitComprovanteFilter() {
        return registrar("rateLimitComprovante", "/api/inscricao/*",
                new RateLimitPorIpFilter(10, Duration.ofMinutes(15),
                        "Muitos envios de comprovante. Aguarde alguns minutos e tente novamente."));
    }

    /* Perguntas do debate sem login (página pública /perguntas). Só conta
       quem vem sem token: com token, o Spring Security já recusa o que não
       for válido e o PerguntaDebateService limita por pessoa — e no debate
       muitos participantes logados saem pelo mesmo IP da universidade. */
    @Bean
    public FilterRegistrationBean<RateLimitPorIpFilter> rateLimitPerguntasFilter() {
        return registrar("rateLimitPerguntas", "/api/perguntas",
                new RateLimitPorIpFilter(20, Duration.ofMinutes(10),
                        "Muitas perguntas a partir desta rede. Aguarde alguns minutos e tente novamente.") {
                    @Override
                    protected boolean shouldNotFilter(HttpServletRequest request) {
                        return super.shouldNotFilter(request) || request.getHeader(HttpHeaders.AUTHORIZATION) != null;
                    }
                });
    }

    private FilterRegistrationBean<RateLimitPorIpFilter> registrar(String nome, String padraoUrl,
                                                                   RateLimitPorIpFilter filtro) {
        FilterRegistrationBean<RateLimitPorIpFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setName(nome);
        registro.addUrlPatterns(padraoUrl);
        return registro;
    }
}
