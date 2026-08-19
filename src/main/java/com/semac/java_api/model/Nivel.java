package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nivel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Nivel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(name = "xp_minimo", nullable = false, unique = true)
    private Integer xpMinimo;
}
