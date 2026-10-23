package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* Partida de uma pessoa num desafio da Cifra do Dia.

   Um jogo por pessoa por desafio (unique pessoa+desafio), mesmo motivo de
   TermoJogo: o xp entra uma vez só e as tentativas não voltam com um F5.
   As 5 tentativas valem para o desafio inteiro, somando as etapas.

   `etapasConcluidas` 0 = ainda na primeira etapa. `venceu` e
   `encerradoEm` ficam null enquanto a partida está em andamento. */
@Entity
@Table(
        name = "desafio_cifra_jogo",
        uniqueConstraints = @UniqueConstraint(
                name = "desafio_cifra_jogo_pessoa_desafio_key",
                columnNames = { "pessoa_id", "desafio_id" }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class DesafioCifraJogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pessoa_id")
    private Pessoa pessoa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "desafio_id")
    private DesafioCifra desafio;

    @Column(name = "etapas_concluidas", nullable = false)
    private Integer etapasConcluidas = 0;

    @Column(name = "tentativas_usadas", nullable = false)
    private Integer tentativasUsadas = 0;

    private Boolean venceu;

    @Column(name = "encerrado_em")
    private LocalDateTime encerradoEm;

    @Column(name = "xp_creditado")
    private Integer xpCreditado;
}
