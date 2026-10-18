package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/* Um dos quatro dias do jogo do Termo (/termo).

   `data` é o que decide qual dia está valendo: o servidor compara com a
   data de hoje. As palavras do dia ficam em TermoPalavra — são várias, e
   cada participante recebe uma por sorteio (ver TermoService). */
@Entity
@Table(
        name = "termo_dia",
        uniqueConstraints = {
                @UniqueConstraint(name = "termo_dia_ano_dia_key", columnNames = { "ano", "dia" }),
                @UniqueConstraint(name = "termo_dia_data_key", columnNames = "data")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class TermoDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer ano;

    /* 1 a 4 — os quatro dias do evento. */
    @Column(name = "dia", nullable = false)
    private Integer numero;

    @Column(nullable = false)
    private LocalDate data;
}
