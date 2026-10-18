package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

/* Uma das palavras secretas de um dia do jogo do Termo (/termo).

   Um dia tem quantas a diretoria cadastrar, de 5 a 10 letras cada, e cada
   participante joga contra uma delas — a que o sorteio lhe deu (ver
   TermoJogo e TermoService.sortearJogo).

   `palavra` nunca sai da API: não existe DTO de resposta que a carregue
   enquanto o jogo está em andamento, nem para a diretoria que a cadastrou
   (mesmo critério de TipoInscricao.codigo, que só expõe `codigoDefinido`).
   Quem confere o palpite é o servidor — ver TermoService. */
@Entity
@Table(
        name = "termo_palavra",
        uniqueConstraints = @UniqueConstraint(
                name = "termo_palavra_dia_palavra_key",
                columnNames = { "dia_id", "palavra" }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class TermoPalavra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dia_id")
    private TermoDia dia;

    @Column(nullable = false, length = 10)
    private String palavra;
}
