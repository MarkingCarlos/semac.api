package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

/* A quantidade de um produto cotado dentro de uma variação, com o
   fornecedor escolhido pra essa variação (pode ser diferente entre
   variações do mesmo conjunto — cada uma varia livremente). Só existe
   linha aqui pros produtos com quantidade informada — matriz esparsa.
   Uma variação nunca repete o mesmo produto (uq_variacao_item). */
@Entity
@Table(name = "variacao_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class VariacaoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "variacao_id", nullable = false)
    private Variacao variacao;

    @ManyToOne
    @JoinColumn(name = "cotacao_id", nullable = false)
    private Cotacao cotacao;

    @ManyToOne
    @JoinColumn(name = "fornecedor_id", nullable = false)
    private Fornecedor fornecedor;

    @Column(nullable = false)
    private Integer quantidade;
}
