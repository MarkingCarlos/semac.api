package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/* Meta de arrecadacao da campanha de doacao — uma linha por ano. Exibida
   na barra de progresso da pagina publica de doacao. Editada no /admin
   (secao "Informações SEMAC"). */
@Entity
@Table(
        name = "meta_doacao",
        uniqueConstraints = @UniqueConstraint(
                name = "meta_doacao_ano_key",
                columnNames = "ano"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class MetaDoacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;
}
