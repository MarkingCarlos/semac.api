package com.semac.java_api.config;

import com.semac.java_api.config.CatalogoConquistas.ConquistaSemeada;
import com.semac.java_api.model.Conquista;
import com.semac.java_api.repository.ConquistaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/* Garante que toda conquista declarada em CatalogoConquistas tenha sua
   linha na tabela `conquista`, para aparecer em /admin -> Informações
   SEMAC e poder ser configurada.

   A divisão de propriedade é o ponto todo deste runner:

     - o código é dono de `codigo` (a ligação com a regra) e de
       `tipoValidacao` (se o sistema avalia ou uma pessoa concede). Esses
       dois são sincronizados a cada boot;

     - o /admin é dono de nome, pontos, imagem, descrição, raridade, ordem
       e `ativa`. Depois da primeira inserção o runner NUNCA toca neles —
       senão todo deploy desfaria a configuração da presidência.

   Conquista nova nasce inativa e com PONTOS_INICIAIS: nada aparece para o
   participante antes de alguém revisar e ativar.

   @Order(1): o catálogo precisa existir antes de o
   ConquistaReavaliacaoRunner tentar conceder qualquer coisa. */
@Component
@Order(1)
public class ConquistaSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ConquistaSeedRunner.class);

    private final ConquistaRepository conquistaRepository;

    public ConquistaSeedRunner(ConquistaRepository conquistaRepository) {
        this.conquistaRepository = conquistaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Set<String> codigosDoCatalogo = new HashSet<>();

        for (ConquistaSemeada semeada : CatalogoConquistas.CONQUISTAS) {
            codigosDoCatalogo.add(semeada.codigo());
            conquistaRepository.findByCodigo(semeada.codigo())
                    .ifPresentOrElse(
                            existente -> sincronizar(existente, semeada),
                            () -> inserir(semeada));
        }

        avisarSobreOrfas(codigosDoCatalogo);
    }

    private void inserir(ConquistaSemeada semeada) {
        Conquista conquista = new Conquista();
        conquista.setCodigo(semeada.codigo());
        conquista.setNome(semeada.nome());
        conquista.setDescricao(semeada.descricao());
        conquista.setTipoValidacao(semeada.tipoValidacao());
        conquista.setRaridade(semeada.raridade());
        conquista.setOrdem(semeada.ordem());
        conquista.setPontosBase(CatalogoConquistas.PONTOS_INICIAIS);
        conquista.setAtiva(false);
        conquistaRepository.save(conquista);

        log.info("Conquista '{}' criada no catálogo (inativa, {} pontos) — configure em /admin.",
                semeada.codigo(), CatalogoConquistas.PONTOS_INICIAIS);
    }

    /* Só `tipoValidacao` — o resto pertence ao /admin (ver cabeçalho). */
    private void sincronizar(Conquista existente, ConquistaSemeada semeada) {
        if (existente.getTipoValidacao() == semeada.tipoValidacao()) {
            return;
        }
        log.info("Conquista '{}' mudou de validação: {} -> {}.",
                semeada.codigo(), existente.getTipoValidacao(), semeada.tipoValidacao());
        existente.setTipoValidacao(semeada.tipoValidacao());
        conquistaRepository.save(existente);
    }

    /* Linha no banco sem entrada correspondente no catálogo: sobra de uma
       conquista removida do código. Não apaga sozinho — pode haver gente
       vinculada a ela, e apagar levaria junto o xp já creditado. Só avisa
       para alguém decidir (revogar e excluir, ou reintroduzir no código). */
    private void avisarSobreOrfas(Set<String> codigosDoCatalogo) {
        conquistaRepository.findAll().stream()
                .filter(conquista -> !codigosDoCatalogo.contains(conquista.getCodigo()))
                .forEach(conquista -> log.warn(
                        "Conquista '{}' (id {}) está no banco mas não no CatalogoConquistas — "
                                + "nenhuma regra vai concedê-la.",
                        conquista.getCodigo(), conquista.getId()));
    }
}
