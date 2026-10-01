package com.semac.java_api.config;

import com.semac.java_api.config.CatalogoTiposEvento.TipoEventoSemeado;
import com.semac.java_api.model.TipoEvento;
import com.semac.java_api.repository.TipoEventoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/* Garante que todo tipo declarado em CatalogoTiposEvento tenha sua linha
   em `tipo_evento`. Mesma divisão de propriedade do ConquistaSeedRunner:
   o código é dono de `codigo`; nome, pontos e exigeInscricao são do
   /admin e nunca são tocados depois da primeira inserção.

   Não há tipo órfão a avisar: `codigo` é NOT NULL e o enum é fechado, então
   uma linha com código fora do enum nem chegaria a ser lida pelo Hibernate.

   @Order(0): os tipos precisam existir antes de qualquer runner que leia
   eventos, como o ConquistaReavaliacaoRunner. */
@Component
@Order(0)
public class TipoEventoSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TipoEventoSeedRunner.class);

    private final TipoEventoRepository tipoEventoRepository;

    public TipoEventoSeedRunner(TipoEventoRepository tipoEventoRepository) {
        this.tipoEventoRepository = tipoEventoRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (TipoEventoSemeado semeado : CatalogoTiposEvento.TIPOS) {
            if (tipoEventoRepository.findByCodigo(semeado.codigo()).isEmpty()) {
                inserir(semeado);
            }
        }
    }

    private void inserir(TipoEventoSemeado semeado) {
        TipoEvento tipo = new TipoEvento();
        tipo.setCodigo(semeado.codigo());
        tipo.setNome(semeado.nome());
        tipo.setPontos(semeado.pontos());
        tipo.setExigeInscricao(semeado.exigeInscricao());
        tipoEventoRepository.save(tipo);

        log.info("Tipo de evento '{}' criado ({} pontos) — ajuste em /admin se preciso.",
                semeado.codigo(), semeado.pontos());
    }
}
