package com.semac.java_api.config;

import com.mercadopago.MercadoPagoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/* Configura o SDK da Mercado Pago com o access token da aplicação assim
   que o contexto sobe — MercadoPagoConfig.setAccessToken é estático e
   vale para toda chamada feita por PaymentClient depois disso. */
@Configuration
public class MercadoPagoSdkConfig {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoSdkConfig.class);

    public MercadoPagoSdkConfig(@Value("${mercadopago.access.token}") String accessToken) {
        MercadoPagoConfig.setAccessToken(accessToken);
        // Diagnóstico temporário: confirma qual token o processo realmente
        // carregou, sem expor o valor inteiro no log. Remover depois.
        log.info("Mercado Pago access token carregado: {}...{} (tamanho {})",
                accessToken.substring(0, Math.min(20, accessToken.length())),
                accessToken.length() > 6 ? accessToken.substring(accessToken.length() - 6) : "",
                accessToken.length());
    }
}
