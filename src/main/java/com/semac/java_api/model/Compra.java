package com.semac.java_api.model;

import com.semac.java_api.model.enums.ContaFinanceira;
import com.semac.java_api.model.enums.StatusCompra;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false, length = 50)
    private String categoria;

    @ManyToOne
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorUnitario;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "data_compra", nullable = false)
    private LocalDateTime dataCompra;

    /* Conta de onde o dinheiro saiu. Null enquanto nao informada. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ContaFinanceira conta;

    /* Uma compra registrada e, por padrao, dinheiro que ja saiu;
       PENDENTE cobre a compra fechada e ainda nao paga. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusCompra status = StatusCompra.PAGO;
}
