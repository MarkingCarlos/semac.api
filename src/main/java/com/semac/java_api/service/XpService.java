package com.semac.java_api.service;

import com.semac.java_api.model.Pessoa;
import com.semac.java_api.repository.NivelRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/* Único ponto que mexe no xp de uma pessoa: presença (check-in), Termo,
   Cifra do Dia, conquistas e Quiz dos Laboratórios passam por aqui.

   Antes cada um lia pessoa.xp, somava e gravava. Dois créditos da mesma
   pessoa no mesmo instante (o quiz e um check-in, por exemplo) liam o
   mesmo valor e o segundo a gravar apagava o primeiro. Agora a linha da
   pessoa é travada (SELECT ... FOR UPDATE) e relida do banco antes da
   soma: quem chega depois espera o primeiro terminar e já soma em cima
   do valor novo. A trava vale até o fim da transação de quem chamou, e
   pessoas diferentes não esperam umas pelas outras. */
@Service
public class XpService {

    @PersistenceContext
    private EntityManager entityManager;

    private final NivelRepository nivelRepository;

    public XpService(NivelRepository nivelRepository) {
        this.nivelRepository = nivelRepository;
    }

    /* Soma `delta` (negativo = estorno) e recalcula o nível. Nunca deixa
       o xp abaixo de zero. Atualiza o próprio objeto recebido quando ele
       já é da sessão, para quem chamou seguir vendo o valor certo. */
    @Transactional
    public void creditar(Pessoa pessoa, int delta) {
        Pessoa travada;
        if (entityManager.contains(pessoa)) {
            // Grava antes o que a sessão tem pendente, senão o refresh descartaria.
            entityManager.flush();
            entityManager.refresh(pessoa, LockModeType.PESSIMISTIC_WRITE);
            travada = pessoa;
        } else {
            travada = entityManager.find(Pessoa.class, pessoa.getId(), LockModeType.PESSIMISTIC_WRITE);
        }

        int xpAtual = travada.getXp() == null ? 0 : travada.getXp();
        int novoXp = Math.max(0, xpAtual + delta);
        travada.setXp(novoXp);
        nivelRepository.findTopByXpMinimoLessThanEqualOrderByXpMinimoDesc(novoXp)
                .ifPresent(travada::setNivel);
        entityManager.flush();

        if (travada != pessoa) {
            pessoa.setXp(travada.getXp());
            pessoa.setNivel(travada.getNivel());
        }
    }
}
