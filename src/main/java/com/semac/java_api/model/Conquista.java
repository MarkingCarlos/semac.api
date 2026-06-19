package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conquista")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Conquista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Integer raridade;

    @Column(name = "imagem_url")
    private String imagemUrl;

    @Column(name = "pontos_base", nullable = false)
    private Integer pontosBase;

    @OneToMany(mappedBy = "conquista")
    private List<ParticipanteConquista> participanteConquistas = new ArrayList<>();
}
