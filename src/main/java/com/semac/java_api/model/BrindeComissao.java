package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* QR do brinde da comissão (ver V74__brinde_comissao.sql). Uma linha por
   pessoa (a chave é o próprio pessoa_id). `retiradoEm` nulo = ainda não
   retirou; preenchido, o código não vale mais. */
@Entity
@Table(name = "brinde_comissao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "pessoaId")
public class BrindeComissao {

    @Id
    @Column(name = "pessoa_id")
    private Integer pessoaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_id", insertable = false, updatable = false)
    private Pessoa pessoa;

    @Column(nullable = false, unique = true, length = 64)
    private String codigo;

    @Column(name = "retirado_em")
    private LocalDateTime retiradoEm;

    /* Quem leu o QR e entregou; nulo se ainda não retirou (ou se quem
       entregou foi excluído). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entregue_por_id")
    private Pessoa entreguePor;
}
