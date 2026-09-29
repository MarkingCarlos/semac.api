package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* Estado da recuperação de senha de uma pessoa — uma linha por pessoa
   (ver V46__recuperacao_senha.sql). As regras de expiração, uso único e
   bloqueio vivem no RecuperacaoSenhaService; aqui só os dados. */
@Entity
@Table(name = "recuperacao_senha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class RecuperacaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false, unique = true)
    private Pessoa pessoa;

    /* Hash SHA-256 do código de 5 dígitos. Nulo depois de usado. */
    @Column(name = "codigo_hash", length = 64)
    private String codigoHash;

    @Column(name = "codigo_enviado_em")
    private LocalDateTime codigoEnviadoEm;

    @Column(name = "codigo_expira_em")
    private LocalDateTime codigoExpiraEm;

    @Column(name = "tentativas_erradas", nullable = false)
    private Integer tentativasErradas = 0;

    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    /* Hash SHA-256 do token que autoriza uma única troca de senha. */
    @Column(name = "token_troca_hash", length = 64)
    private String tokenTrocaHash;

    @Column(name = "token_troca_expira_em")
    private LocalDateTime tokenTrocaExpiraEm;
}
