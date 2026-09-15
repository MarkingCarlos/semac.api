package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

/* A edição da SEMAC. Guarda só o ano.

   Nenhum parâmetro do orçamento é digitado: o teto vem do saldo da
   comissão e os três multiplicadores de escala vêm de contagens no banco
   (inscritos, membros da comissão e palestrantes) — ver PrevisaoService.
   Um número digitado ao lado deles ficaria defasado sem ninguém notar,
   que foi exatamente o que aconteceu enquanto eram campos.

   Não há teto aqui: ele é derivado do saldo da conta da comissão, em
   PrevisaoService.resumo(). Um teto digitado ao lado de um derivado
   discordaria do caixa na primeira inscrição que entrasse. */
@Entity
@Table(name = "orcamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private Integer ano;

}
