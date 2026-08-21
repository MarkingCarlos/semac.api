package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tipo_evento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class TipoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Integer pontos;

    /* true = o participante escolhe entrar (minicurso, vagas limitadas);
       false = evento aberto, em que todo participante confirmado é
       pré-inscrito automaticamente. Ver V7__inscricao_minicurso.sql. */
    @Column(name = "exige_inscricao", nullable = false)
    private Boolean exigeInscricao = false;

    @OneToMany(mappedBy = "tipoEvento")
    private List<Evento> eventos = new ArrayList<>();
}
