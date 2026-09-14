package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/* Categoria de gasto previsto — cada sub-tabela da planilha de previsão
   (Coffee Break, Kit do Participante, Passagem, Hospedagem...) é uma
   linha aqui, e não uma tabela própria. Por isso criar uma categoria
   nova não exige migration. */
@Entity
@Table(name = "previsao_categoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PrevisaoCategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 80, unique = true)
    private String nome;

    /* Cor usada nos gráficos e no selo da categoria na tabela. */
    @Column(nullable = false, length = 20)
    private String cor;

    /* Limite próprio da categoria. Null = sujeita apenas ao teto global. */
    @Column(precision = 10, scale = 2)
    private BigDecimal teto;

    @Column(nullable = false)
    private Integer ordem = 0;
}
