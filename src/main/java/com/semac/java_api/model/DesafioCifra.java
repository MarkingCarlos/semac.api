package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/* Um dia da Cifra do Dia (/desafio-cifra): a senha criptografada que
   aparece na tela e as etapas para decifrá-la.

   `data` decide qual desafio está valendo, como em TermoDia. A senha é
   pública; as respostas ficam nas etapas e não saem do servidor (ver
   DesafioCifraService). */
@Entity
@Table(name = "desafio_cifra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class DesafioCifra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false, unique = true)
    private LocalDate data;

    @Column(nullable = false)
    private String palestra;

    @Column(name = "senha_cifrada", nullable = false)
    private String senhaCifrada;

    @OneToMany(mappedBy = "desafio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<DesafioCifraEtapa> etapas = new ArrayList<>();
}
