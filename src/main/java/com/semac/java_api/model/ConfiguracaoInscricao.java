package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* Liga/desliga o botão "Inscreva-se" da Home — uma linha por ano. Editada
   no /admin (seção "Informações SEMAC"). Puramente visual: não bloqueia
   o cadastro em si, só a chamada pública de entrada nele. */
@Entity
@Table(
        name = "configuracao_inscricao",
        uniqueConstraints = @UniqueConstraint(
                name = "configuracao_inscricao_ano_key",
                columnNames = "ano"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ConfiguracaoInscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Boolean inscricoesAbertas;

    /* Libera a escolha de minicursos no /participantes (/admin, aba
       Conteúdo). Fechada, ninguém entra nem sai de minicurso — quem já
       estava inscrito continua inscrito. Inicializado aqui porque o PUT
       do botão "Inscreva-se" pode criar a linha do ano sem mexer nele. */
    @Column(nullable = false)
    private Boolean escolhaMinicursosAberta = Boolean.FALSE;

    /* Libera o envio de perguntas para os debates no /participantes
       (/admin, Informações SEMAC). Fechado, o botão some e o backend
       recusa novas perguntas — as já enviadas continuam no /admin. */
    @Column(nullable = false)
    private Boolean perguntasDebateAbertas = Boolean.FALSE;

    /* Libera a revelação do time em /time (/admin, aba Times). Desligado,
       quem escaneia o QR code vai para o /participantes sem ver nada. */
    @Column(nullable = false)
    private Boolean timesAtivos = Boolean.FALSE;

    /* Revelação programada: a partir deste horário os times valem como
       liberados mesmo com `timesAtivos` desligado. Nulo = só o interruptor
       decide (ver V73__times_revelacao_programada.sql). */
    @Column(name = "times_revelacao_em")
    private LocalDateTime timesRevelacaoEm;

    /* Esconde o ranking de XP — dos participantes e dos times — no
       /participantes e no /time (/admin, Informações SEMAC > Gamificação).
       O XP do próprio participante continua visível; o card de ranking da
       dashboard do /admin (diretoria) não é afetado. */
    @Column(nullable = false)
    private Boolean rankingOculto = Boolean.FALSE;
}
