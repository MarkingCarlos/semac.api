package com.semac.java_api.model;

import com.semac.java_api.model.pk.EventoPalestrantePK;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evento_palestrante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "pk")
public class EventoPalestrante {

    @EmbeddedId
    private EventoPalestrantePK pk = new EventoPalestrantePK();

    @ManyToOne
    @MapsId("eventoId")
    @JoinColumn(name = "evento_id")
    private Evento evento;

    @ManyToOne
    @MapsId("palestranteId")
    @JoinColumn(name = "palestrante_id")
    private Palestrante palestrante;
}
