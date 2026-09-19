package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* Texto de uma mensagem automática, editável em /admin -> Mensagens.
   A `chave` liga a linha à entrada correspondente no CatalogoVariaveisEmail,
   que define quais variáveis o corpo pode usar. */
@Entity
@Table(name = "modelo_email")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ModeloEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 60)
    private String chave;

    @Column(nullable = false, length = 200)
    private String assunto;

    @Column(name = "corpo_markdown", nullable = false, columnDefinition = "TEXT")
    private String corpoMarkdown;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atualizado_por_id")
    private Pessoa atualizadoPor;
}
