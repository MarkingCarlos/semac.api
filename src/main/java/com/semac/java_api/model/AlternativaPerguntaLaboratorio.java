package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

/* Uma alternativa de uma pergunta do quiz. `correta` nunca vai para o
   participante antes de ele responder. */
@Entity
@Table(name = "alternativa_pergunta_laboratorio")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class AlternativaPerguntaLaboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pergunta_id", nullable = false)
    private PerguntaLaboratorio pergunta;

    @Column(nullable = false)
    private Integer ordem;

    @Column(nullable = false, length = 300)
    private String texto;

    @Column(nullable = false)
    private Boolean correta = false;
}
