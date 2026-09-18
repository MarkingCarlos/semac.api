package com.semac.java_api.model;

import com.semac.java_api.model.pk.ParticipanteConquistaPK;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "participante_conquista")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "pk")
public class ParticipanteConquista {

    @EmbeddedId
    private ParticipanteConquistaPK pk = new ParticipanteConquistaPK();

    @ManyToOne
    @MapsId("participanteId")
    @JoinColumn(name = "participante_id")
    private Pessoa participante;

    @ManyToOne
    @MapsId("conquistaId")
    @JoinColumn(name = "conquista_id")
    private Conquista conquista;

    @Column(name = "obtida_em", nullable = false)
    private LocalDateTime obtidaEm;

    /* Quanto essa conquista somou em pessoa.xp no momento da concessão —
       mesmo papel de EventoParticipante.xpCreditado. Guardado porque
       revogar precisa estornar o valor que foi creditado de fato, não o
       que o catálogo vale hoje: os pontos podem ter sido editados no
       /admin no meio do caminho. */
    @Column(name = "xp_creditado")
    private Integer xpCreditado;

    /* Quem da diretoria concedeu, nas conquistas manuais. Null nas
       automáticas — ali quem concedeu foi o sistema. */
    @ManyToOne
    @JoinColumn(name = "concedida_por_id")
    private Pessoa concedidaPor;

    /* Quando a animação de "conquista desbloqueada" foi exibida a esta
       pessoa. Null = conquistada mas ainda não celebrada, ou seja, entra
       na fila da animação na próxima vez que ela abrir /participantes.

       É o que garante o "uma vez só" (ver V36). Fica no banco, e não no
       navegador, para valer em qualquer dispositivo. */
    @Column(name = "vista_em")
    private LocalDateTime vistaEm;
}
