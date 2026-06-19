package com.semac.java_api.model;

import com.semac.java_api.model.pk.ParticipanteConquistaPK;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "participante_conquista")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "pk")
public class ParticipanteConquista {

    @EmbeddedId
    private ParticipanteConquistaPK pk = new ParticipanteConquistaPK();

    @ManyToOne
    @MapsId("participanteId")
    @JoinColumn(name = "participante_id")
    private Pessoa participante;

    @ManyToOne
    @MapsId("conquistaId")
    @JoinColumn(name = "conquista_id")
    private Conquista conquista;

    @Column(name = "obtida_em", nullable = false)
    private LocalDateTime obtidaEm;
}
