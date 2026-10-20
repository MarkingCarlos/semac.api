package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* Um participante leu o QR code do stand de um trabalho da Mostra Técnica.

   Uma por pessoa por trabalho (unique no banco): ler o mesmo QR de novo
   não conta duas vezes. É a contagem destas visitas que decide a
   conquista MOSTRA_COMPLETA (ver ConquistaService.avaliarMostraCompleta). */
@Entity
@Table(
        name = "visita_trabalho_mostra",
        uniqueConstraints = @UniqueConstraint(
                name = "visita_trabalho_mostra_pessoa_trabalho_key",
                columnNames = { "pessoa_id", "trabalho_id" }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class VisitaTrabalhoMostra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pessoa_id")
    private Pessoa pessoa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trabalho_id")
    private TrabalhoMostra trabalho;

    @Column(name = "visitada_em", nullable = false)
    private LocalDateTime visitadaEm;
}
