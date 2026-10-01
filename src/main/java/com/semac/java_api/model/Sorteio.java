package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/* Sorteio cadastrado no /admin (aba Brindes → Sorteios): nome + evento
   onde acontece. Um evento pode ter vários sorteios; cada brinde
   pertence a um sorteio, e cada entrega fica em `ganhadores_sorteio`. */
@Entity
@Table(name = "sorteio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Sorteio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    @ManyToOne
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @OneToMany(mappedBy = "sorteio")
    private List<Brinde> brindes = new ArrayList<>();

    @OneToMany(mappedBy = "sorteio")
    private List<GanhadoresSorteio> ganhadores = new ArrayList<>();
}
