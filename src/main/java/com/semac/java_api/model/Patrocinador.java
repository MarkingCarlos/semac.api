package com.semac.java_api.model;

import com.semac.java_api.model.enums.StatusPagamento;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/* Mesma entidade exibida no site e usada no controle financeiro.
   Campos de exibição: nome, descricao, logoUrl, cota (nível).
   Campos financeiros: desconto, adicao, valorFinal, statusPagamento,
   dataRecebimento, observacao. O valorFinal é calculado pelo backend
   a partir da cota (cota.valor − desconto + adicao). */
@Entity
@Table(name = "patrocinador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Patrocinador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    @Column(name = "logo_url")
    private String logoUrl;

    @ManyToOne
    @JoinColumn(name = "cota_id", nullable = false)
    private Cota cota;

    @Column(precision = 10, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal adicao = BigDecimal.ZERO;

    @Column(name = "valor_final", precision = 10, scale = 2)
    private BigDecimal valorFinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pagamento", nullable = false)
    private StatusPagamento statusPagamento = StatusPagamento.A_RECEBER;

    @Column(name = "data_recebimento")
    private LocalDateTime dataRecebimento;

    private String observacao;
}
