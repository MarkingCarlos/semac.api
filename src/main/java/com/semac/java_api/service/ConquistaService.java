package com.semac.java_api.service;

import com.semac.java_api.model.Conquista;
import com.semac.java_api.model.ParticipanteConquista;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.model.pk.ParticipanteConquistaPK;
import com.semac.java_api.repository.ConquistaRepository;
import com.semac.java_api.repository.NivelRepository;
import com.semac.java_api.repository.ParticipanteConquistaRepository;
import com.semac.java_api.repository.PessoaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/* Regras de conquista — cada uma é um método hardcoded, mesmo padrão já usado
   nas regras de xp (ver InscricaoEventoService.calcularXpCreditado). Sem
   motor de regras genérico: conquista nova = uma linha no catálogo
   (`conquista.codigo`) + um método aqui, chamado do ponto certo. */
@Service
public class ConquistaService {

    private static final Logger log = LoggerFactory.getLogger(ConquistaService.class);

    private static final String CODIGO_PRIMEIROS_10 = "PRIMEIROS_10";
    private static final int LIMITE_PRIMEIROS_10 = 10;

    private final PessoaRepository pessoaRepository;
    private final ConquistaRepository conquistaRepository;
    private final ParticipanteConquistaRepository participanteConquistaRepository;
    private final NivelRepository nivelRepository;

    public ConquistaService(PessoaRepository pessoaRepository,
                            ConquistaRepository conquistaRepository,
                            ParticipanteConquistaRepository participanteConquistaRepository,
                            NivelRepository nivelRepository) {
        this.pessoaRepository = pessoaRepository;
        this.conquistaRepository = conquistaRepository;
        this.participanteConquistaRepository = participanteConquistaRepository;
        this.nivelRepository = nivelRepository;
    }

    /* "Primeiros 10 confirmados": recalcula do zero quem são os 10
       participantes (role = PARTICIPANTE) mais antigos por inscrito_em e
       concede a quem ainda não tem. Idempotente e sem estado próprio — pode
       rodar de novo (boot, ou a cada confirmação) sem duplicar nem depender
       de quando cada um foi avaliado antes. */
    @Transactional
    public void avaliarPrimeirosDezConfirmados() {
        List<Pessoa> primeiros = pessoaRepository.findByRoleOrderByInscritoEmAsc(Role.PARTICIPANTE).stream()
                .limit(LIMITE_PRIMEIROS_10)
                .toList();
        for (Pessoa pessoa : primeiros) {
            conceder(pessoa, CODIGO_PRIMEIROS_10);
        }
    }

    /* Vincula a conquista à pessoa (se ainda não tiver) e credita o xp base
       dela, recalculando o nível — mesmo critério de
       InscricaoEventoService.creditarXp. Não falha se o catálogo ainda não
       tiver essa conquista semeada: só loga e não faz nada. */
    private void conceder(Pessoa pessoa, String codigo) {
        Conquista conquista = conquistaRepository.findByCodigo(codigo).orElse(null);
        if (conquista == null) {
            log.warn("Conquista de código '{}' não encontrada no catálogo — pulando concessão.", codigo);
            return;
        }

        if (participanteConquistaRepository.existsByPk_ParticipanteIdAndPk_ConquistaId(pessoa.getId(), conquista.getId())) {
            return;
        }

        ParticipanteConquista vinculo = new ParticipanteConquista();
        vinculo.setPk(new ParticipanteConquistaPK(pessoa.getId(), conquista.getId()));
        vinculo.setParticipante(pessoa);
        vinculo.setConquista(conquista);
        vinculo.setObtidaEm(LocalDateTime.now());
        participanteConquistaRepository.save(vinculo);

        creditarXp(pessoa, conquista.getPontosBase());
        log.info("Conquista '{}' concedida a {} (id {}).", codigo, pessoa.getNome(), pessoa.getId());
    }

    private void creditarXp(Pessoa pessoa, int pontos) {
        int xpAtual = pessoa.getXp() == null ? 0 : pessoa.getXp();
        int novoXp = xpAtual + pontos;
        pessoa.setXp(novoXp);
        nivelRepository.findTopByXpMinimoLessThanEqualOrderByXpMinimoDesc(novoXp)
                .ifPresent(pessoa::setNivel);
        pessoaRepository.save(pessoa);
    }
}
