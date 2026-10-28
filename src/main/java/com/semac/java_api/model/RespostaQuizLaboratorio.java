package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* A tentativa (única) de uma pessoa no quiz de um laboratório. Guarda só o
   placar e o XP dado; a gravação é por INSERT ... ON CONFLICT (ver
   RespostaQuizLaboratorioRepository.registrar). */
@Entity
@Table(name = "resposta_quiz_laboratorio")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class RespostaQuizLaboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "pessoa_id", nullable = false)
    private Integer pessoaId;

    @Column(name = "laboratorio_id", nullable = false)
    private Integer laboratorioId;

    @Column(nullable = false)
    private Integer acertos;

    @Column(nullable = false)
    private Integer total;

    @Column(name = "xp_creditado", nullable = false)
    private Integer xpCreditado;

    @Column(name = "respondido_em", nullable = false)
    private LocalDateTime respondidoEm;
}
