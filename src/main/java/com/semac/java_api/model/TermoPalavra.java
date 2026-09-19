package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/* Palavra secreta de um dia do jogo do Termo (/termo).

   `palavra` nunca sai da API: não existe DTO de resposta que a carregue
   enquanto o jogo está em andamento, nem para a diretoria que a cadastrou
   (mesmo critério de TipoInscricao.codigo, que só expõe `codigoDefinido`).
   Quem confere o palpite é o servidor — ver TermoService.

   `data` é o que decide qual palavra está valendo: o servidor compara com
   a data de hoje. */
@Entity
@Table(
        name = "termo_palavra",
        uniqueConstraints = {
                @UniqueConstraint(name = "termo_palavra_ano_dia_key", columnNames = { "ano", "dia" }),
                @UniqueConstraint(name = "termo_palavra_data_key", columnNames = "data")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class TermoPalavra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer ano;

    /* 1 a 4 — os quatro dias do evento. */
    @Column(nullable = false)
    private Integer dia;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false, length = 5)
    private String palavra;
}
