package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "criptografia_jogo_acerto",
        uniqueConstraints = @UniqueConstraint(
                name = "criptografia_jogo_pessoa_palavra_key",
                columnNames = { "pessoa_id", "palavra_id" }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class CriptografiaJogoAcerto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pessoa_id")
    private Pessoa pessoa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "palavra_id")
    private CriptografiaPalavra palavra;
    
}
