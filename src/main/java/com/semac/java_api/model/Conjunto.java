package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/* Conjunto de cotação (ex.: "Coffee") — agrupa variações comparáveis
   (ex.: "Coffee Cheio", "Coffee Reduzido"), cada uma com suas próprias
   quantidades por item cotado. */
@Entity
@Table(name = "conjunto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Conjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    @OneToMany(mappedBy = "conjunto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Variacao> variacoes = new ArrayList<>();
}
