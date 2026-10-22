package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "professor_convidado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ProfessorConvidado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;
}
