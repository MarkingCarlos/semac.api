package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/* Item sendo cotado (ainda não comprado). Pode ter várias linhas de
   fornecedor com preços diferentes (cotacao_fornecedor). Quando a compra
   é fechada, o registro é cadastrado manualmente em Compra e removido
   daqui — não há conversão automática. */
@Entity
@Table(name = "cotacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Cotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false, length = 50)
    private String categoria;

    @Column(nullable = false)
    private Integer quantidade;

    @OneToMany(mappedBy = "cotacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CotacaoFornecedor> fornecedores = new ArrayList<>();
}
