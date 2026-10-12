package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* Pergunta enviada previamente por um participante para um evento do tipo
   DEBATE (ver V52__pergunta_debate.sql). As regras — só debate, só com as
   perguntas liberadas, limite por participante — vivem no
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    @Column(nullable = false, length = 500)
    private String texto;

    @Column(name = "enviada_em", nullable = false)
    private LocalDateTime enviadaEm;
}
