package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ganhadores_sorteio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "sorteioId")
public class GanhadoresSorteio {

    @Id
    @Column(name = "sorteio_id")
    private Integer sorteioId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "sorteio_id")
    private Sorteio sorteio;

    @ManyToOne
    @JoinColumn(name = "participante_id", nullable = false)
    private Pessoa participante;

    @Column(name = "ganhou_em", nullable = false)
    private LocalDateTime ganhouEm;
}
