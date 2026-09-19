package com.semac.java_api.model;

import com.semac.java_api.model.enums.UnidadeRegraXp;
import jakarta.persistence.*;
import lombok.*;

/* Uma regra de xp que não está amarrada a um tipo de evento: o acerto do
   Termo do dia e os dois limites de atraso do check-in. O xp de presença
   continua em TipoEvento.pontos — ver V40__regra_xp.sql.

   `chave` é o que o código procura (RegraXpService.CHAVE_*) e nunca é
   editável; o admin mexe só em `nome` e `valor`. Linhas não nascem nem
   morrem pela API: cada chave tem código que a lê. */
@Entity
@Table(name = "regra_xp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class RegraXp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, updatable = false)
    private String chave;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Integer valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnidadeRegraXp unidade;

    @Column(nullable = false)
    private Integer ordem;
}
