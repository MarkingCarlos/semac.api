package com.semac.java_api.config;

import com.semac.java_api.service.ConquistaService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/* Reavalia as conquistas automáticas de todo participante a cada boot.

   Cobre o caso que o check-in sozinho não cobre: uma conquista ativada
   depois que gente já cumpriu a regra. Sem isso, quem completou a semana
   antes de a presidência ativar a conquista nunca a receberia.

   Roda depois do ConquistaSeedRunner (ver @Order) — não adianta tentar
   conceder uma conquista que ainda não está no catálogo. Idempotente:
   quem já tem é pulado, e conquista inativa não é concedida.

   Durante a semana do evento, o equivalente sem reiniciar a API é o botão
   "Reavaliar conquistas" em /admin (POST /api/conquista/reavaliar). */
@Component
@Order(2)
public class ConquistaReavaliacaoRunner implements CommandLineRunner {

    private final ConquistaService conquistaService;

    public ConquistaReavaliacaoRunner(ConquistaService conquistaService) {
        this.conquistaService = conquistaService;
    }

    @Override
    public void run(String... args) {
        conquistaService.reavaliarAutomaticasDeTodos();
    }
}
