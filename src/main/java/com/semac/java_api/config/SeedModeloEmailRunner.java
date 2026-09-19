package com.semac.java_api.config;

import com.semac.java_api.config.CatalogoVariaveisEmail.ModeloSemeado;
import com.semac.java_api.model.ModeloEmail;
import com.semac.java_api.repository.ModeloEmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/* Garante que toda mensagem declarada no CatalogoVariaveisEmail tenha sua
   linha em `modelo_email`, para aparecer em /admin -> Mensagens.

   Mesma divisão de propriedade do ConquistaSeedRunner: insere o que falta
   e NUNCA sobrescreve o que já existe. Se a comissão reescreveu o texto da
   confirmação, o próximo deploy não desfaz isso.

   Rodar a cada boot é o que faz uma mensagem nova (adicionada ao catálogo
   junto com seu gatilho) aparecer no /admin sem migration nenhuma. */
@Component
@Order(1)
public class SeedModeloEmailRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedModeloEmailRunner.class);

    private final ModeloEmailRepository modeloEmailRepository;

    public SeedModeloEmailRunner(ModeloEmailRepository modeloEmailRepository) {
        this.modeloEmailRepository = modeloEmailRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int inseridos = 0;
        for (ModeloSemeado semeado : CatalogoVariaveisEmail.todos()) {
            if (modeloEmailRepository.findByChave(semeado.chave()).isPresent()) {
                continue;
            }
            ModeloEmail modelo = new ModeloEmail();
            modelo.setChave(semeado.chave());
            modelo.setAssunto(semeado.assuntoPadrao());
            modelo.setCorpoMarkdown(semeado.corpoPadrao());
            modelo.setAtivo(true);
            modeloEmailRepository.save(modelo);
            inseridos++;
        }
        if (inseridos > 0) {
            log.info("Modelos de e-mail semeados: {}.", inseridos);
        }
    }
}
