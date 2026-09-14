package com.semac.java_api.config;

import com.semac.java_api.service.ConquistaService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/* Reavalia as conquistas hardcoded a cada boot (ver ConquistaService) — cobre
   participantes que já foram confirmados antes de uma regra existir, sem
   precisar de migration de dado ou script manual. Idempotente: quem já tem a
   conquista é pulado. */
@Component
public class ConquistaBackfillRunner implements CommandLineRunner {

    private final ConquistaService conquistaService;

    public ConquistaBackfillRunner(ConquistaService conquistaService) {
        this.conquistaService = conquistaService;
    }

    @Override
    public void run(String... args) {
        conquistaService.avaliarPrimeirosDezConfirmados();
    }
}
