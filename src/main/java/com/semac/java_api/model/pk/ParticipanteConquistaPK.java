package com.semac.java_api.model.pk;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ParticipanteConquistaPK implements Serializable {

    @Column(name = "participante_id")
    private Integer participanteId;

    @Column(name = "conquista_id")
    private Integer conquistaId;
}
