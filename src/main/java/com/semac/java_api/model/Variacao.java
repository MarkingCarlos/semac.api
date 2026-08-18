package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/* Uma variação dentro de um conjunto (ex.: "Coffee Cheio") — uma coluna
   da grade de comparação, com suas próprias quantidades por item. */
@Entity
@Table(name = "variacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Variacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "conjunto_id", nullable = false)
    private Conjunto conjunto;

    @Column(nullable = false)
    private String nome;

    @OneToMany(mappedBy = "variacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VariacaoItem> itens = new ArrayList<>();
}
