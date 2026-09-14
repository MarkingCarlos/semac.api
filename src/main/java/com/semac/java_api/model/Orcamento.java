package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/* Parâmetros do orçamento de uma edição. Além do teto de gastos, guarda
   os contadores que a planilha tratava como constantes soltas nas notas
   ("estimativa total: 140 pessoas", "comissão: 39 membros") e que aqui
   alimentam a escala de PrevisaoItem. */
@Entity
@Table(name = "orcamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private Integer ano;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal teto;

    @Column(name = "inscritos_previstos", nullable = false)
    private Integer inscritosPrevistos = 0;

    @Column(name = "membros_comissao", nullable = false)
    private Integer membrosComissao = 0;

    @Column(name = "palestrantes_previstos", nullable = false)
    private Integer palestrantesPrevistos = 0;
}
