package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/* Saldo repassado pela FundoUnesp de edições anteriores da SEMAC.
   Registro único por edição — a tabela tem uma linha só, editada na aba
   Resumo do módulo financeiro. Não entra no cálculo do saldo operacional;
   é exibido em card separado. */
@Entity
@Table(name = "caixa_fundunesp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class CaixaFundunesp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    /* Quem realizou o último ajuste. Null quando a linha foi semeada
       direto no banco, sem passar pela interface. */
    @ManyToOne
    @JoinColumn(name = "atualizado_por")
    private Pessoa atualizadoPor;
}
