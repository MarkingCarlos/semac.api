package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* Um trabalho exposto num stand da Mostra Técnica.

   `codigoQr` é o que vai no link impresso no stand (/mostra/<codigoQr>).
   É aleatório de propósito: com o id na URL daria para chutar os outros
   trabalhos sem passar pelos stands. Ver TrabalhoMostraService. */
@Entity
@Table(name = "trabalho_mostra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class TrabalhoMostra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false, length = 200)
    private String titulo;

    /* Texto livre — quem apresenta não precisa ter conta no sistema. */
    @Column(nullable = false, length = 300)
    private String apresentadores;

    @Column(length = 50)
    private String stand;

    @Column(name = "codigo_qr", nullable = false, unique = true, length = 32)
    private String codigoQr;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
