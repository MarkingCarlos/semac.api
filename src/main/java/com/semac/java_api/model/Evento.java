package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "data_hora_inicio", nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(name = "data_hora_fim", nullable = false)
    private LocalDateTime dataHoraFim;

    private String local;

    @Column(name = "capacidade_maxima", nullable = false)
    private Integer capacidadeMaxima;

    private String descricao;

    @ManyToOne
    @JoinColumn(name = "tipo_evento_id", nullable = false)
    private TipoEvento tipoEvento;

    @OneToMany(mappedBy = "evento")
    private List<EventoParticipante> eventoParticipantes = new ArrayList<>();

    @OneToMany(mappedBy = "evento")
    private List<EventoPalestrante> eventoPalestrantes = new ArrayList<>();

    @OneToMany(mappedBy = "evento")
    private List<Sorteio> sorteios = new ArrayList<>();
}
