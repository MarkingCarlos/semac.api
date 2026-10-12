package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

/* Um dos três times da SEMAC. Nascem na migration V53 com nome e cor do
   design; nome e mascote são editados no /admin (aba Times), a cor não.
   `mascoteArquivo` guarda só o nome do arquivo em disco (PNG ou SVG),
   mesmo padrão da imagem de conquista. */
@Entity
@Table(name = "time_semac")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Time {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 40)
    private String nome;

    @Column(nullable = false, length = 7)
    private String cor;

    @Column(name = "mascote_arquivo")
    private String mascoteArquivo;
}
