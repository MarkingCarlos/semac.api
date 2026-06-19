package com.semac.java_api.model;

import com.semac.java_api.model.enums.StatusPresenca;
import com.semac.java_api.model.pk.EventoParticipantePK;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evento_participante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "pk")
public class EventoParticipante {

    @EmbeddedId
    private EventoParticipantePK pk = new EventoParticipantePK();

    @ManyToOne
    @MapsId("eventoId")
    @JoinColumn(name = "evento_id")
    private Evento evento;

    @ManyToOne
    @MapsId("participanteId")
    @JoinColumn(name = "participante_id")
    private Pessoa participante;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private StatusPresenca status;
}
