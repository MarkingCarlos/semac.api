package com.semac.java_api.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/* Liga o RateLimitCartaoFilter só na rota de cobrança do cartão — não
   afeta o resto da API. */
@Configuration
public class RateLimitConfig {

    @Bean
    public FilterRegistrationBean<RateLimitCartaoFilter> rateLimitCartaoFilter() {
        FilterRegistrationBean<RateLimitCartaoFilter> registro = new FilterRegistrationBean<>();
        registro.setFilter(new RateLimitCartaoFilter());
        registro.addUrlPatterns("/api/pagamento/cartao");
        return registro;
    }
}
