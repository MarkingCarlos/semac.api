package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* Lugar no pódio da tarefa de um minicurso — base da conquista manual
   ALQUIMIA_COMPLETA (ver V77__vitoria_tarefa_minicurso.sql). Uma por
   pessoa e minicurso, no máximo 3 por minicurso. `xpCreditado` é 0
   quando a pessoa já tinha a conquista de outro dia. As regras vivem no
   ConquistaService; aqui só os dados. */
@Entity
@Table(name = "vitoria_tarefa_minicurso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class VitoriaTarefaMinicurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    @Column(name = "concedida_em", nullable = false)
    private LocalDateTime concedidaEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concedida_por_id")
    private Pessoa concedidaPor;

    @Column(name = "xp_creditado", nullable = false)
    private Integer xpCreditado;
}
