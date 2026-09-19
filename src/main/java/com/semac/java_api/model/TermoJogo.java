package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/* Partida de uma pessoa contra a palavra de um dia.

   Um jogo por pessoa por dia (unique pessoa+palavra): é o que garante que
   a vitória credite os 5 xp uma vez só, e que as 6 tentativas não voltem
   com um F5.

   `venceu` e `encerradoEm` ficam null enquanto a partida está em
   andamento. `xpCreditado` guarda quanto foi creditado de fato, mesma
   convenção de EventoParticipante.xpCreditado e
   ParticipanteConquista.xpCreditado. */
@Entity
@Table(
        name = "termo_jogo",
        uniqueConstraints = @UniqueConstraint(
                name = "termo_jogo_pessoa_palavra_key",
                columnNames = { "pessoa_id", "palavra_id" }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class TermoJogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pessoa_id")
    private Pessoa pessoa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "palavra_id")
    private TermoPalavra palavra;

    private Boolean venceu;

    @Column(name = "encerrado_em")
    private LocalDateTime encerradoEm;

    @Column(name = "xp_creditado")
    private Integer xpCreditado;

    @OneToMany(mappedBy = "jogo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<TermoTentativa> tentativas = new ArrayList<>();
}
