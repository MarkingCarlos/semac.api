package com.semac.java_api.model;

import com.semac.java_api.model.enums.ContaFinanceira;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "doador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Doador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDateTime data;

    /* Conta que recebe a doacao. Null enquanto nao informada. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ContaFinanceira conta;
}
