package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

/* Uma etapa de um desafio da Cifra do Dia. A de maior `ordem` é a
   resposta final; as anteriores são passos intermediários (a senha
   parcialmente descriptografada, no desafio de segunda).

   `resposta` guarda os espaços: são eles que dividem as casas do campo na
   tela. Na comparação eles somem (ver DesafioCifraService.normalizar). */
@Entity
@Table(
        name = "desafio_cifra_etapa",
        uniqueConstraints = @UniqueConstraint(
                name = "desafio_cifra_etapa_ordem_key",
                columnNames = { "desafio_id", "ordem" }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class DesafioCifraEtapa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "desafio_id")
    private DesafioCifra desafio;

    @Column(nullable = false)
    private Integer ordem;

    @Column(nullable = false)
    private String resposta;

    @Column(nullable = false)
    private String dica;
}
