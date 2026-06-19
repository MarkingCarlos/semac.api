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
public class EventoPalestrantePK implements Serializable {

    @Column(name = "evento_id")
    private Integer eventoId;

    @Column(name = "palestrante_id")
    private Integer palestranteId;
}
