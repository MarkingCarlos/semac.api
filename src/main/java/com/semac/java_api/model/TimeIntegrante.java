package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* Resultado do sorteio: em qual time cada participante caiu. Uma linha por
   pessoa (a chave é o próprio pessoa_id). `reveladoEm` marca a primeira
   vez que a pessoa abriu /time — o /admin usa para contar quem já viu. */
@Entity
@Table(name = "time_integrante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "pessoaId")
public class TimeIntegrante {

    @Id
    @Column(name = "pessoa_id")
    private Integer pessoaId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "time_id")
    private Time time;

    @Column(name = "revelado_em")
    private LocalDateTime reveladoEm;
}
