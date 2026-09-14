package com.semac.java_api.model;

import com.semac.java_api.model.enums.ContaFinanceira;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/* Saldo inicial de cada conta, remanescente de edições anteriores da
   SEMAC. Uma linha por conta (COMISSAO e FUNDUNESP), editada na aba
   Resumo do módulo financeiro. Não entra no cálculo do saldo
   operacional; é exibido em card separado.

   Até a V28 a tabela se chamava `caixa_fundunesp` e guardava só o saldo
   da FUNDUNESP — o nome deixou de descrevê-la quando passou a comportar
   as duas contas. */
@Entity
@Table(name = "caixa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Caixa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, unique = true)
    private ContaFinanceira conta;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    /* Quem realizou o último ajuste. Null quando a linha foi semeada
       direto no banco, sem passar pela interface. */
    @ManyToOne
    @JoinColumn(name = "atualizado_por")
    private Pessoa atualizadoPor;
}
