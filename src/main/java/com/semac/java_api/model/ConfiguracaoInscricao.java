package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

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
}
