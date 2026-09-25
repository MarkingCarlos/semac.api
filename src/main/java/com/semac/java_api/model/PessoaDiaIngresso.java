package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/* Um dia em que o ingresso diário da pessoa vale. Quem comprou N diárias
   tem N linhas aqui, escolhidas em /participantes (ver
   DiaIngressoService). O check-in de um diarista só passa nesses dias. */
@Entity
@Table(
        name = "pessoa_dia_ingresso",
        uniqueConstraints = @UniqueConstraint(
                name = "pessoa_dia_ingresso_pessoa_dia_key",
                columnNames = {"pessoa_id", "dia"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PessoaDiaIngresso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "pessoa_id", nullable = false)
    private Integer pessoaId;

    @Column(nullable = false)
    private LocalDate dia;
}
