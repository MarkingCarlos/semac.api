package com.semac.java_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/* Pool dedicado ao envio de e-mail. Separado do executor padrão do Spring
   porque SMTP é lento e instável: uma fila entupida de e-mail não pode
   segurar nenhum outro trabalho assíncrono da aplicação.

   Uma thread só, de propósito — envio em série também serve de freio
   natural contra o limite diário do Gmail (~500 destinatários/dia). */
@Configuration
@EnableAsync
public class AsyncConfig {

    /* Pool separado do de cima, e essa separação é o ponto: um comunicado
       para 400 pessoas leva minutos. Na mesma fila, o e-mail de "inscrição
       confirmada" de quem acabou de ser confirmado ficaria esperando o
       lote inteiro terminar. */
    @Bean(name = "executorComunicado")
    public Executor executorComunicado() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("comunicado-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Bean(name = "executorEmail")
    public Executor executorEmail() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("email-");
        /* Espera a fila esvaziar no shutdown: e-mail de confirmação já
           aceito não pode sumir porque o container reiniciou. */
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
