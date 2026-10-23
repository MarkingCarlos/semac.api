package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* Pergunta enviada previamente para um evento do tipo DEBATE (ver
   V52__pergunta_debate.sql e V62__pergunta_debate_sem_login.sql). Com
   login, `pessoa` aponta quem enviou; sem login, `pessoa` é nula e
   `nomeAutor` guarda o nome digitado (opcional). Incluída pelo /admin,
   `cadastradaPor` aponta o diretor/presidente que digitou. As regras — só debate, só
   com as perguntas liberadas, limite por pessoa — vivem no
   PerguntaDebateService; aqui só os dados. */
@Entity
@Table(name = "pergunta_debate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PerguntaDebate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /* Nula quando a pergunta veio sem login (página pública /perguntas). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_id")
    private Pessoa pessoa;

    /* Nome digitado por quem enviou sem login; nulo quando há `pessoa` ou
       quando a pessoa preferiu não se identificar. */
    @Column(name = "nome_autor", length = 120)
    private String nomeAutor;

    /* Diretor/presidente que incluiu a pergunta pelo /admin; nulo quando
       veio do público (ver V67__pergunta_debate_cadastrada_por.sql). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cadastrada_por_id")
    private Pessoa cadastradaPor;

    @Column(nullable = false, length = 500)
    private String texto;

    @Column(name = "enviada_em", nullable = false)
    private LocalDateTime enviadaEm;
}
