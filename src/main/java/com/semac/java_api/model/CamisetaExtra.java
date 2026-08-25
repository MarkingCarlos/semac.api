package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/* Preço da camiseta avulsa de uma edição — uma linha por ano. Quem quer
   mais camisetas do que o ingresso dá direito paga esse valor por unidade.
   Editado no /admin (seção "Informações SEMAC"). */
@Entity
@Table(
        name = "camiseta_extra",
        uniqueConstraints = @UniqueConstraint(
                name = "camiseta_extra_ano_key",
                columnNames = "ano"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class CamisetaExtra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;
}
