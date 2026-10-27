package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/* Retirada do brinde (Monster) em um dia (ver V74__brinde_comissao.sql e
   V78__brinde_por_dia.sql). Uma linha por pessoa por dia.

   Comissão: a linha nasce quando a pessoa abre o próprio QR no /admin,
   com `codigo` preenchido e `retiradoEm` nulo; preenchido, o código não
   vale mais. Participante: não tem QR próprio (retira pelo crachá), então
   a linha já nasce retirada e sem `codigo`. */
@Entity
@Table(name = "brinde_comissao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class BrindeComissao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "pessoa_id", nullable = false)
    private Integer pessoaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_id", insertable = false, updatable = false)
    private Pessoa pessoa;

    @Column(nullable = false)
    private LocalDate dia;

    @Column(unique = true, length = 64)
    private String codigo;

    @Column(name = "retirado_em")
    private LocalDateTime retiradoEm;

    /* Quem leu o QR e entregou; nulo se ainda não retirou (ou se quem
       entregou foi excluído). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entregue_por_id")
    private Pessoa entreguePor;
}
