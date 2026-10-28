package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/* Um laboratório da Mostra Técnica, com o quiz que ele preparou (ver
   V79__laboratorio_quiz.sql).

   `codigoQr` é o que vai no link impresso (/laboratorio/<codigoQr>). É
   aleatório de propósito, como o da Mostra: com o id na URL daria para
   chutar os outros. As perguntas são salvas junto com o laboratório (a
   edição troca a lista inteira). Ver LaboratorioQuizService. */
@Entity
@Table(name = "laboratorio")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Laboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false, length = 200)
    private String nome;

    /* Texto livre — quem responde pelo laboratório não precisa ter conta. */
    @Column(length = 300)
    private String responsaveis;

    @Column(name = "codigo_qr", nullable = false, unique = true, length = 32)
    private String codigoQr;

    @Column(name = "quiz_aberto", nullable = false)
    private Boolean quizAberto = false;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "laboratorio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<PerguntaLaboratorio> perguntas = new ArrayList<>();
}
