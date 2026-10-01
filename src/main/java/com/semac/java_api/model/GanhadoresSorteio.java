package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* Uma entrega de brinde: quem ganhou, qual brinde, em qual sorteio e
   quem realizou o sorteio (organizador, vindo do token). */
@Entity
@Table(name = "ganhadores_sorteio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class GanhadoresSorteio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "sorteio_id", nullable = false)
    private Sorteio sorteio;

    @ManyToOne
    @JoinColumn(name = "brinde_id", nullable = false)
    private Brinde brinde;

    @ManyToOne
    @JoinColumn(name = "participante_id", nullable = false)
    private Pessoa participante;

    @ManyToOne
    @JoinColumn(name = "organizador_id", nullable = false)
    private Pessoa organizador;

    @Column(name = "ganhou_em", nullable = false)
    private LocalDateTime ganhouEm;
}
