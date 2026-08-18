package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/* Preço de um fornecedor para um item de cotação (cotacao_id + fornecedor_id
   únicos — um fornecedor aparece no máximo uma vez por item). */
@Entity
@Table(name = "cotacao_fornecedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class CotacaoFornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "cotacao_id", nullable = false)
    private Cotacao cotacao;

    @ManyToOne
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorUnitario;
}
