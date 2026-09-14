package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

/* Parâmetros do orçamento de uma edição: os contadores que a planilha
   tratava como constantes soltas nas notas ("comissão: 39 membros") e
   que aqui alimentam a escala de PrevisaoItem.

   O número de inscritos NÃO está aqui: é derivado da contagem de pessoas
   com role PARTICIPANTE ou NULL (ver PrevisaoService).-

   Não há teto aqui: ele é derivado do saldo da conta da comissão, em
   PrevisaoService.resumo(). Um teto digitado ao lado de um derivado
   discordaria do caixa na primeira inscrição que entrasse. */
@Entity
@Table(name = "orcamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private Integer ano;

    @Column(name = "membros_comissao", nullable = false)
    private Integer membrosComissao = 0;

    @Column(name = "palestrantes_previstos", nullable = false)
    private Integer palestrantesPrevistos = 0;
}
