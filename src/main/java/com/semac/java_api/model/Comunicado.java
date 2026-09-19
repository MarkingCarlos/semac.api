package com.semac.java_api.model;

import com.semac.java_api.model.enums.PublicoComunicado;
import com.semac.java_api.model.enums.StatusComunicado;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* Um disparo avulso já acontecido. Cada linha é histórico, não modelo:
   e-mail não tem desfazer, então este registro é o que permite saber
   depois o que foi enviado, para quantas pessoas e com que resultado. */
@Entity
@Table(name = "comunicado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Comunicado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String assunto;

    @Column(name = "corpo_markdown", nullable = false, columnDefinition = "TEXT")
    private String corpoMarkdown;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PublicoComunicado publico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private Evento evento;

    /* Congelado no disparo: o público muda com o tempo. */
    @Column(name = "total_destinatarios", nullable = false)
    private Integer totalDestinatarios;

    @Column(nullable = false)
    private Integer enviados = 0;

    @Column(nullable = false)
    private Integer falhas = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusComunicado status;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enviado_por_id")
    private Pessoa enviadoPor;
}
