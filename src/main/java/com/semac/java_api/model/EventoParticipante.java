package com.semac.java_api.model;

import com.semac.java_api.model.enums.StatusPresenca;
import com.semac.java_api.model.pk.EventoParticipantePK;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    /* Momento em que o check-in foi feito e o xp efetivamente creditado
       naquele check-in (0, metade ou cheio — ver
       InscricaoEventoService.marcarPresente). Null até a presença ser
       marcada. */
    @Column(name = "presenca_em")
    private LocalDateTime presencaEm;

    @Column(name = "xp_creditado")
    private Integer xpCreditado;
}
