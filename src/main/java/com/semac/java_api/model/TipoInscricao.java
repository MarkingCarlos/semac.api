package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "tipo_inscricao",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_tipo_inscricao_nome_ano",
                columnNames = {"nome", "ano"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class TipoInscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Boolean ativo = true;

    /* Quantas camisetas gratuitas este ingresso inclui. 0 = sem direito;
       o participante ainda pode comprar avulsas (ver CamisetaExtra). */
    @Column(name = "camisetas_gratis", nullable = false)
    private Integer camisetasGratis = 0;

    /* Ingresso cobrado por diária: o valor acima é o preço de UM dia e o
       total sai de valor x dias escolhidos. `maxDias` limita a escolha. */
    @Column(name = "por_dia", nullable = false)
    private Boolean porDia = false;

    @Column(name = "max_dias")
    private Integer maxDias;
}
