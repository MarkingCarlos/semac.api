package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* Uma leitura de QR (ou confirmação manual) recusada por estar fora da
   janela de check-in do evento — ver
   InscricaoEventoService.ANTECEDENCIA_MAXIMA_CHECKIN_MINUTOS.

   Tudo que identifica evento, participante e operador é cópia do momento
   da tentativa, sem relação JPA: o log precisa continuar legível mesmo
   que o evento ou a pessoa sejam excluídos depois (ver
   V42__tentativa_checkin_bloqueada.sql). */
@Entity
@Table(name = "tentativa_checkin_bloqueada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class TentativaCheckinBloqueada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "evento_id", nullable = false)
    private Integer eventoId;

    @Column(name = "evento_nome", nullable = false)
    private String eventoNome;

    @Column(name = "participante_id", nullable = false)
    private Integer participanteId;

    @Column(name = "participante_nome", nullable = false)
    private String participanteNome;

    @Column(name = "operador_id", nullable = false)
    private Integer operadorId;

    @Column(name = "operador_nome", nullable = false)
    private String operadorNome;

    @Column(name = "operador_role", nullable = false)
    private String operadorRole;

    @Column(name = "tentado_em", nullable = false)
    private LocalDateTime tentadoEm;

    /* Minutos entre a tentativa e a abertura da janela. Separa o "chegou
       cedo demais" do "tentou marcar presença no dia anterior". */
    @Column(name = "minutos_antes", nullable = false)
    private Long minutosAntes;
}
