package com.semac.java_api.model;

import com.semac.java_api.model.enums.TipoValidacaoConquista;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/* Catálogo de conquistas. Cada linha é a metade editável de uma conquista
   cuja regra vive em código: o `codigo` casa esta linha com a entrada
   correspondente em CatalogoConquistas, e o ConquistaSeedRunner garante
   que toda conquista implementada tenha a sua (ver V35).

   A divisão é essa: o código decide *quando* a conquista é dada; o /admin
   decide *como ela se apresenta* (nome, pontos, imagem, descrição) e *se
   já vale* (`ativa`). Por isso nome/pontos/imagem/descrição nunca são
   sobrescritos pelo seeder depois da primeira inserção. */
@Entity
@Table(name = "conquista")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Conquista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    /* Tier visual do card na área do participante (1 comum a 5 lendária).
       NÃO entra no cálculo de pontos: o que vale é pontosBase puro, que é
       o número que a presidência digita no /admin. A doc antiga do Notion
       dizia `pontos_base × raridade`, mas o código nunca multiplicou. */
    @Column(nullable = false)
    private Integer raridade;

    @Column(name = "imagem_url")
    private String imagemUrl;

    /* Enquadramento da imagem na moldura dos cards: ponto da imagem que
       fica em evidência (em %) e zoom (1 a 3), ver V76. A imagem original
       não é recortada. Trocar a imagem volta para o padrão — ver
       ConquistaService.definirImagem. */
    @Column(name = "imagem_pos_x", nullable = false, precision = 5, scale = 2)
    private BigDecimal imagemPosX = ENQUADRAMENTO_CENTRO;

    @Column(name = "imagem_pos_y", nullable = false, precision = 5, scale = 2)
    private BigDecimal imagemPosY = ENQUADRAMENTO_CENTRO;

    @Column(name = "imagem_zoom", nullable = false, precision = 4, scale = 2)
    private BigDecimal imagemZoom = BigDecimal.ONE;

    public static final BigDecimal ENQUADRAMENTO_CENTRO = BigDecimal.valueOf(50);

    @Column(name = "pontos_base", nullable = false)
    private Integer pontosBase;

    /* Chave estável usada pelo backend pra conceder essa conquista (ver
       ConquistaService) sem depender do id numérico — mesmo padrão de
       TipoInscricao.codigo. Única e obrigatória desde a V35. */
    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    /* "Como conseguir", exibido no card mesmo enquanto a conquista está
       bloqueada — é a meta que o participante persegue. */
    @Column(length = 500)
    private String descricao;

    /* Só conquista ativa aparece para o participante e só ela pode ser
       concedida. Nasce false para nada vazar antes de a presidência
       revisar. Desativar é bloqueado enquanto houver alguém com ela
       (ver ConquistaService) — para destravar, revoga-se primeiro. */
    @Column(nullable = false)
    private Boolean ativa = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_validacao", nullable = false, length = 20)
    private TipoValidacaoConquista tipoValidacao = TipoValidacaoConquista.AUTOMATICA;

    /* Posição na grade da área do participante. */
    @Column(nullable = false)
    private Integer ordem = 0;

    @OneToMany(mappedBy = "conquista")
    private List<ParticipanteConquista> participanteConquistas = new ArrayList<>();
}
