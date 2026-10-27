package com.semac.java_api.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* Nota de 0 a 10 que um participante deu a um evento na pesquisa de
   satisfação (ver V75__avaliacao_evento.sql). Uma por pessoa e evento:
   reenviar atualiza `nota` e `atualizadaEm`. As regras — quais tipos
   entram, presença confirmada, só depois do fim — vivem no
   AvaliacaoEventoService; aqui só os dados. */
@Entity
@Table(name = "avaliacao_evento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class AvaliacaoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer nota;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;
}
