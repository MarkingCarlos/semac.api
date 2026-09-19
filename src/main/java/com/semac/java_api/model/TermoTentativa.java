package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* Um palpite gasto numa partida.

   `resultado` é o padrão de cores compactado, uma letra por posição:
   C = certo, P = presente, A = ausente. É o único retorno que o navegador
   recebe — a palavra secreta não aparece aqui nem na resposta da API. */
@Entity
@Table(
        name = "termo_tentativa",
        uniqueConstraints = @UniqueConstraint(
                name = "termo_tentativa_jogo_ordem_key",
                columnNames = { "jogo_id", "ordem" }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class TermoTentativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "jogo_id")
    private TermoJogo jogo;

    /* 1 a 6, na ordem em que foram enviadas. */
    @Column(nullable = false)
    private Integer ordem;

    @Column(nullable = false, length = 5)
    private String palpite;

    @Column(nullable = false, length = 5)
    private String resultado;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
